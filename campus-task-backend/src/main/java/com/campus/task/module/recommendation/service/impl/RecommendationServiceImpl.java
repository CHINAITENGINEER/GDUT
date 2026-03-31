package com.campus.task.module.recommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.task.common.enums.TaskStatus;
import com.campus.task.common.exception.BusinessException;
import com.campus.task.common.utils.SnowflakeUtil;
import com.campus.task.module.recommendation.dto.RecommendationProfileDTO;
import com.campus.task.module.recommendation.entity.RecommendWeightLog;
import com.campus.task.module.recommendation.mapper.RecommendWeightLogMapper;
import com.campus.task.module.recommendation.service.RecommendationService;
import com.campus.task.module.recommendation.util.TaskTagExtractor;
import com.campus.task.module.recommendation.vo.RecommendationProfileVO;
import com.campus.task.module.recommendation.vo.RecommendedTaskVO;
import com.campus.task.module.task.entity.Task;
import com.campus.task.module.task.entity.TaskCategory;
import com.campus.task.module.task.mapper.GrabRecordMapper;
import com.campus.task.module.task.mapper.TaskCategoryMapper;
import com.campus.task.module.task.mapper.TaskMapper;
import com.campus.task.module.task.mapper.UserViolationMapper;
import com.campus.task.module.task.vo.TaskCardVO;
import com.campus.task.module.user.entity.User;
import com.campus.task.module.user.mapper.UserMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final int DEFAULT_DAILY_LIMIT = 10;
    private static final int MAX_CANDIDATE_TASKS = 300;
    private static final BigDecimal LEARNING_RATE = new BigDecimal("0.04");
    private static final BigDecimal WEIGHT_MIN    = new BigDecimal("0.05");
    private static final BigDecimal WEIGHT_MAX    = new BigDecimal("0.55");
    private static final int CREDIT_BASELINE = 100;
    private static final int CREDIT_MAX      = 200;
    private static final long URGENCY_HOT_HOURS  = 24;
    private static final long URGENCY_WARM_HOURS = 72;
    private static final long URGENCY_COLD_HOURS = 168;

    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "的","了","是","在","和","与","或","对","有","一","不",
            "也","都","为","以","要","就","可以","这","那","它",
            "我","你","他","她","们","来","去","上","下","到",
            "从","被","把","让","比","如","等","该","其","此",
            "by","to","of","in","on","an","a","the"
    ));

    private static final BigDecimal[] LEVEL_AMOUNT_CAP = {
            BigDecimal.ZERO, new BigDecimal("80"), new BigDecimal("150"),
            new BigDecimal("250"), new BigDecimal("400"), new BigDecimal("600"), new BigDecimal("9999")
    };

    private final UserMapper userMapper;
    private final TaskMapper taskMapper;
    private final TaskCategoryMapper taskCategoryMapper;
    private final GrabRecordMapper grabRecordMapper;
    private final UserViolationMapper userViolationMapper;
    private final RecommendWeightLogMapper recommendWeightLogMapper;
    private final ObjectMapper objectMapper;
    private final SnowflakeUtil snowflakeUtil;

    @Value("${platform.grab-daily-limit:5}")
    private int grabDailyLimit;
    @Value("${platform.violation-daily-limit:3}")
    private int violationDailyLimit;

    // =========================================================
    // 核心评分方法
    // =========================================================

    private BigDecimal abilityScore(Task task, List<String> userTags) {
        List<String> taskTags = normalizeTags(extractTaskTags(task));
        List<String> uTags    = normalizeTags(userTags);
        if (taskTags.isEmpty() || uTags.isEmpty()) return new BigDecimal("0.50");
        Set<String> taskSet = new HashSet<>(taskTags);
        Set<String> userSet = new HashSet<>(uTags);
        long intersection = userSet.stream().filter(taskSet::contains).count();
        if (intersection == 0) return new BigDecimal("0.30");
        double ratio = (double) intersection / Math.min(taskSet.size(), userSet.size());
        return BigDecimal.valueOf(0.30 + 0.70 * Math.min(ratio, 1.0)).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal categoryScore(Task task, List<Integer> preferredCategories) {
        if (preferredCategories == null || preferredCategories.isEmpty()) return new BigDecimal("0.50");
        return preferredCategories.contains(task.getCategory()) ? BigDecimal.ONE : BigDecimal.ZERO;
    }

    private BigDecimal amountScore(Task task, User user) {
        BigDecimal amt = task.getAmount();
        if (amt == null) return new BigDecimal("0.50");
        BigDecimal lo = user.getMinAcceptAmount();
        BigDecimal hi = user.getMaxAcceptAmount();
        if (lo == null && hi == null) return new BigDecimal("0.70");
        if (lo != null && amt.compareTo(lo) < 0) {
            BigDecimal ref = lo.compareTo(BigDecimal.ZERO) > 0 ? lo : BigDecimal.ONE;
            BigDecimal decay = BigDecimal.ONE.subtract(lo.subtract(amt).divide(ref, 4, RoundingMode.HALF_UP));
            return new BigDecimal("0.50").multiply(decay.max(BigDecimal.ZERO)).setScale(4, RoundingMode.HALF_UP);
        }
        if (hi != null && amt.compareTo(hi) > 0) {
            BigDecimal ref = hi.compareTo(BigDecimal.ZERO) > 0 ? hi : BigDecimal.ONE;
            BigDecimal decay = BigDecimal.ONE.subtract(amt.subtract(hi).divide(ref, 4, RoundingMode.HALF_UP));
            return new BigDecimal("0.50").multiply(decay.max(BigDecimal.ZERO)).setScale(4, RoundingMode.HALF_UP);
        }
        if (lo != null && hi != null && hi.compareTo(lo) > 0) {
            BigDecimal mid  = lo.add(hi).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
            BigDecimal half = hi.subtract(lo).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
            BigDecimal dist = amt.subtract(mid).abs();
            BigDecimal ratio = dist.divide(half, 4, RoundingMode.HALF_UP);
            return BigDecimal.ONE.subtract(ratio.multiply(new BigDecimal("0.10"))).max(new BigDecimal("0.90")).setScale(4, RoundingMode.HALF_UP);
        }
        return new BigDecimal("0.90");
    }

    private BigDecimal deliveryScore(Task task, User user) {
        if (user.getPreferredDeliveryType() == null) return new BigDecimal("0.70");
        return user.getPreferredDeliveryType().equals(task.getDeliveryType()) ? BigDecimal.ONE : new BigDecimal("0.40");
    }

    private BigDecimal urgencyScore(Task task) {
        if (task.getDeadline() == null) return BigDecimal.ZERO;
        long hoursLeft = ChronoUnit.HOURS.between(LocalDateTime.now(ZONE), task.getDeadline());
        if (hoursLeft <= 0 || hoursLeft > URGENCY_COLD_HOURS) return BigDecimal.ZERO;
        if (hoursLeft <= URGENCY_HOT_HOURS) {
            double bonus = 0.07 + 0.03 * (1.0 - (double) hoursLeft / URGENCY_HOT_HOURS);
            return BigDecimal.valueOf(bonus).setScale(4, RoundingMode.HALF_UP);
        }
        if (hoursLeft <= URGENCY_WARM_HOURS) {
            double ratio = (double)(hoursLeft - URGENCY_HOT_HOURS) / (URGENCY_WARM_HOURS - URGENCY_HOT_HOURS);
            return BigDecimal.valueOf(Math.max(0.02, 0.07 - 0.05 * ratio)).setScale(4, RoundingMode.HALF_UP);
        }
        double ratio = (double)(hoursLeft - URGENCY_WARM_HOURS) / (URGENCY_COLD_HOURS - URGENCY_WARM_HOURS);
        return BigDecimal.valueOf(Math.max(0, 0.02 * (1.0 - ratio))).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal creditBonusScore(Task task, Map<Long, User> publisherCache) {
        User publisher = publisherCache.get(task.getPublisherId());
        if (publisher == null || publisher.getCreditScore() == null) return BigDecimal.ZERO;
        int credit = publisher.getCreditScore();
        if (credit <= CREDIT_BASELINE) return BigDecimal.ZERO;
        double ratio = Math.min(1.0, (double)(credit - CREDIT_BASELINE) / (CREDIT_MAX - CREDIT_BASELINE));
        return BigDecimal.valueOf(0.05 * ratio).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal levelMatchScore(Task task, User user) {
        if (user.getLevel() == null || task.getAmount() == null) return BigDecimal.ZERO;
        int level = Math.min(Math.max(user.getLevel(), 1), 6);
        BigDecimal lo  = LEVEL_AMOUNT_CAP[level - 1];
        BigDecimal hi  = LEVEL_AMOUNT_CAP[level];
        BigDecimal amt = task.getAmount();
        if (amt.compareTo(lo) >= 0 && amt.compareTo(hi) <= 0) return new BigDecimal("0.05");
        BigDecimal mid = lo.add(hi).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
        BigDecimal ref = hi.subtract(lo).compareTo(BigDecimal.ZERO) > 0 ? hi.subtract(lo) : BigDecimal.ONE;
        BigDecimal decay = BigDecimal.ONE.subtract(amt.subtract(mid).abs().divide(ref, 4, RoundingMode.HALF_UP));
        return new BigDecimal("0.05").multiply(decay.max(BigDecimal.ZERO)).setScale(4, RoundingMode.HALF_UP);
    }

    // =========================================================
    // 推荐理由构建
    // =========================================================

    private String buildReason(BigDecimal sAbility, BigDecimal sCategory,
                               BigDecimal sAmount, BigDecimal sDelivery,
                               BigDecimal urgencyBonus, BigDecimal levelBonus, Task task) {
        List<String> reasons = new ArrayList<>();
        if (sCategory.compareTo(BigDecimal.ONE) == 0) reasons.add("符合你的偏好分类");
        if (sAbility.compareTo(new BigDecimal("0.75")) >= 0) reasons.add("技能高度契合");
        else if (sAbility.compareTo(new BigDecimal("0.50")) >= 0) reasons.add("技能标签相关");
        if (sAmount.compareTo(new BigDecimal("0.90")) >= 0) reasons.add("佣金在你的期望区间内");
        else if (sAmount.compareTo(new BigDecimal("0.70")) >= 0) reasons.add("佣金较为合理");
        if (sDelivery.compareTo(BigDecimal.ONE) == 0) reasons.add("符合你的交付偏好");
        if (urgencyBonus.compareTo(new BigDecimal("0.07")) >= 0) reasons.add("截止日期临近，尽快接单");
        else if (urgencyBonus.compareTo(new BigDecimal("0.03")) >= 0) reasons.add("任务即将截止");
        if (levelBonus.compareTo(new BigDecimal("0.04")) >= 0) reasons.add("契合你当前的接单段位");
        if (reasons.isEmpty()) return "与你的画像整体匹配";
        return String.join(" · ", reasons.stream().limit(3).collect(Collectors.toList()));
    }

    // =========================================================
    // 权重梯度计算
    // =========================================================

    private Map<String, BigDecimal> computeGradient(Task task, User user,
                                                     List<String> abilityTags,
                                                     List<Integer> preferredCategories) {
        BigDecimal sAbility  = abilityScore(task, abilityTags);
        BigDecimal sCategory = categoryScore(task, preferredCategories);
        BigDecimal sAmount   = amountScore(task, user);
        BigDecimal sDelivery = deliveryScore(task, user);
        BigDecimal center = new BigDecimal("0.5");
        Map<String, BigDecimal> g = new LinkedHashMap<>();
        g.put("ability",  sAbility.subtract(center).multiply(BigDecimal.valueOf(2)));
        g.put("amount",   sAmount.subtract(center).multiply(BigDecimal.valueOf(2)));
        g.put("category", sCategory.compareTo(BigDecimal.ONE) == 0 ? BigDecimal.ONE : new BigDecimal("-0.60"));
        g.put("delivery", sDelivery.compareTo(BigDecimal.ONE) == 0 ? new BigDecimal("0.80") : new BigDecimal("-0.40"));
        return g;
    }

    // =========================================================
    // 过滤 / 候选集获取 / 冷启动
    // =========================================================

    private boolean receivable(Task task, User user) {
        LocalDateTime now = LocalDateTime.now(ZONE);
        if (task.getDeadline() != null && task.getDeadline().isBefore(now)) return false;
        BigDecimal amt = task.getAmount();
        if (amt != null) {
            if (user.getMinAcceptAmount() != null && amt.compareTo(user.getMinAcceptAmount()) < 0) return false;
            if (user.getMaxAcceptAmount() != null && amt.compareTo(user.getMaxAcceptAmount()) > 0) return false;
        }
        return true;
    }

    private boolean blocked(Long userId) {
        LocalDate today = LocalDate.now(ZONE);
        long grabs = grabRecordMapper.selectCount(
                new LambdaQueryWrapper<com.campus.task.module.task.entity.GrabRecord>()
                        .eq(com.campus.task.module.task.entity.GrabRecord::getUserId, userId)
                        .eq(com.campus.task.module.task.entity.GrabRecord::getGrabDate, today));
        if (grabs >= grabDailyLimit) return true;
        long violations = userViolationMapper.selectCount(
                new LambdaQueryWrapper<com.campus.task.module.task.entity.UserViolation>()
                        .eq(com.campus.task.module.task.entity.UserViolation::getUserId, userId)
                        .eq(com.campus.task.module.task.entity.UserViolation::getViolationDate, today));
        return violations >= violationDailyLimit;
    }

    private List<Task> fetchCandidates(Long userId) {
        List<Long> grabbedIds = grabRecordMapper.selectList(
                new LambdaQueryWrapper<com.campus.task.module.task.entity.GrabRecord>()
                        .eq(com.campus.task.module.task.entity.GrabRecord::getUserId, userId)
                        .select(com.campus.task.module.task.entity.GrabRecord::getTaskId)
        ).stream().map(com.campus.task.module.task.entity.GrabRecord::getTaskId).collect(Collectors.toList());
        LambdaQueryWrapper<Task> qw = new LambdaQueryWrapper<Task>()
                .eq(Task::getStatus, TaskStatus.PENDING_GRAB.getCode())
                .orderByDesc(Task::getCreatedAt)
                .last("LIMIT " + MAX_CANDIDATE_TASKS);
        if (!grabbedIds.isEmpty()) qw.notIn(Task::getId, grabbedIds);
        return taskMapper.selectList(qw);
    }

    private List<RecommendedTaskVO> coldStartRecommend(User user, int limit) {
        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getStatus, TaskStatus.PENDING_GRAB.getCode())
                        .orderByDesc(Task::getCreatedAt)
                        .last("LIMIT " + Math.max(limit * 3, 30)));
        Map<Long, User> publisherCache = batchLoadPublishers(tasks);
        Map<Integer, String> categoryNames = buildCategoryNameMap();
        return tasks.stream()
                .filter(t -> receivable(t, user))
                .map(t -> {
                    BigDecimal urgency = urgencyScore(t);
                    BigDecimal credit  = creditBonusScore(t, publisherCache);
                    RecommendedTaskVO vo = new RecommendedTaskVO();
                    vo.setTask(toCardVO(t, categoryNames, publisherCache));
                    vo.setScore(new BigDecimal("0.50").add(urgency).add(credit).setScale(4, RoundingMode.HALF_UP).min(BigDecimal.ONE));
                    vo.setRecommendReason(urgency.compareTo(new BigDecimal("0.05")) >= 0 ? "截止日期临近，尽快接单" : "热门新任务，快来接单");
                    Map<String, BigDecimal> bd = new LinkedHashMap<>();
                    bd.put("coldStart", BigDecimal.ONE);
                    bd.put("urgencyBonus", urgency);
                    bd.put("creditBonus", credit);
                    vo.setScoreBreakdown(bd);
                    return vo;
                })
                .sorted(Comparator.comparing(RecommendedTaskVO::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private Map<Long, User> batchLoadPublishers(List<Task> tasks) {
        if (tasks.isEmpty()) return Collections.emptyMap();
        List<Long> ids = tasks.stream().map(Task::getPublisherId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) return Collections.emptyMap();
        return userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, ids))
                .stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
    }

    private Map<Integer, String> buildCategoryNameMap() {
        return taskCategoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(TaskCategory::getId, TaskCategory::getName, (a, b) -> a));
    }




    private Map<String, BigDecimal> defaultWeights() {
        Map<String, BigDecimal> m = new LinkedHashMap<>();
        m.put("ability",  new BigDecimal("0.35"));
        m.put("category", new BigDecimal("0.30"));
        m.put("amount",   new BigDecimal("0.25"));
        m.put("delivery", new BigDecimal("0.10"));
        return m;
    }

    private Map<String, BigDecimal> normalize(Map<String, BigDecimal> raw) {
        BigDecimal sum = raw.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(BigDecimal.ZERO) == 0) return defaultWeights();
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        List<String> keys = new ArrayList<>(raw.keySet());
        BigDecimal acc = BigDecimal.ZERO;
        for (int i = 0; i < keys.size() - 1; i++) {
            BigDecimal v = raw.get(keys.get(i)).divide(sum, 6, RoundingMode.HALF_UP);
            result.put(keys.get(i), v);
            acc = acc.add(v);
        }
        result.put(keys.get(keys.size() - 1), BigDecimal.ONE.subtract(acc).max(BigDecimal.ZERO));
        return result;
    }

    private Map<String, BigDecimal> parseWeights(String json) {
        if (!StringUtils.hasText(json)) return defaultWeights();
        try {
            Map<String, BigDecimal> m = objectMapper.readValue(json, new TypeReference<Map<String, BigDecimal>>() {});
            Map<String, BigDecimal> def = defaultWeights();
            for (String k : def.keySet()) m.putIfAbsent(k, def.get(k));
            return normalize(m);
        } catch (Exception e) { return defaultWeights(); }
    }

    private BigDecimal w(Map<String, BigDecimal> weights, String key) {
        return weights.getOrDefault(key, defaultWeights().get(key));
    }

    private <T> List<T> parseList(String json, TypeReference<List<T>> ref) {
        if (!StringUtils.hasText(json)) return Collections.emptyList();
        try {
            List<T> list = objectMapper.readValue(json, ref);
            return list != null ? list : Collections.emptyList();
        } catch (Exception e) { return Collections.emptyList(); }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return null; }
    }

    private User getUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        return user;
    }

    private List<String> extractTaskTags(Task task) {
        return TaskTagExtractor.extract(task.getTitle(), task.getDescription());
    }

    private List<String> normalizeTags(List<String> tags) {
        return tags.stream().filter(Objects::nonNull)
                .map(s -> s.trim().toLowerCase())
                .filter(s -> !s.isEmpty())
                .filter(s -> !STOPWORDS.contains(s))
                .collect(Collectors.toList());
    }

    private RecommendationProfileVO toProfileVO(User user) {
        RecommendationProfileVO vo = new RecommendationProfileVO();
        vo.setAbilityTags(parseList(user.getAbilityTags(), new TypeReference<List<String>>() {}));
        vo.setPreferredCategoryIds(parseList(user.getPreferredCategories(), new TypeReference<List<Integer>>() {}));
        vo.setPreferredDeliveryType(user.getPreferredDeliveryType());
        vo.setMinAcceptAmount(user.getMinAcceptAmount());
        vo.setMaxAcceptAmount(user.getMaxAcceptAmount());
        vo.setDailyRecommendLimit(user.getDailyRecommendLimit() != null ? user.getDailyRecommendLimit() : DEFAULT_DAILY_LIMIT);
        vo.setWeights(parseWeights(user.getRecommendWeights()));
        return vo;
    }

    private TaskCardVO toCardVO(Task task, Map<Integer, String> categoryNames, Map<Long, User> publisherCache) {
        TaskCardVO vo = new TaskCardVO();
        vo.setId(task.getId());
        vo.setTitle(task.getTitle());
        vo.setCategory(task.getCategory());
        vo.setCategoryName(categoryNames.getOrDefault(task.getCategory(), "其他"));
        vo.setAmount(task.getAmount());
        vo.setDeliveryType(task.getDeliveryType());
        vo.setDeadline(task.getDeadline() != null ? task.getDeadline().atZone(ZONE).toInstant().toEpochMilli() : null);
        vo.setStatus(task.getStatus());
        vo.setCreatedAt(task.getCreatedAt() != null ? task.getCreatedAt().atZone(ZONE).toInstant().toEpochMilli() : null);
        User publisher = publisherCache.get(task.getPublisherId());
        if (publisher != null) {
            TaskCardVO.PublisherInfo pi = new TaskCardVO.PublisherInfo();
            pi.setId(publisher.getId());
            pi.setNickname(publisher.getNickname());
            pi.setAvatar(publisher.getAvatar());
            pi.setCreditScore(publisher.getCreditScore());
            vo.setPublisher(pi);
        }
        vo.setTaskImages(parseList(task.getTaskImages(), new TypeReference<List<String>>() {}));
        return vo;
    }

    private void logWeightChange(Long userId, Long taskId, Map<String, BigDecimal> before, Map<String, BigDecimal> after, int triggerType, String reason) {
        try {
            RecommendWeightLog log = new RecommendWeightLog();
            log.setId(snowflakeUtil.nextId());
            log.setUserId(userId); log.setTaskId(taskId); log.setTriggerType(triggerType);
            log.setBeforeWeights(toJson(before)); log.setAfterWeights(toJson(after));
            log.setTriggerReason(reason); log.setCreatedAt(LocalDateTime.now(ZONE));
            recommendWeightLogMapper.insert(log);
        } catch (Exception ignored) {}
    }


    // =========================================================
    // 接口实现：画像 CRUD
    // =========================================================

    @Override
    public RecommendationProfileVO getProfile(Long userId) {
        User user = getUser(userId);
        return toProfileVO(user);
    }

    @Override
    @Transactional
    public RecommendationProfileVO saveProfile(Long userId, RecommendationProfileDTO dto) {
        User user = getUser(userId);
        if (dto.getAbilityTags() != null)          user.setAbilityTags(toJson(dto.getAbilityTags()));
        if (dto.getPreferredCategoryIds() != null)  user.setPreferredCategories(toJson(dto.getPreferredCategoryIds()));
        if (dto.getPreferredDeliveryType() != null) user.setPreferredDeliveryType(dto.getPreferredDeliveryType());
        if (dto.getMinAcceptAmount() != null)       user.setMinAcceptAmount(dto.getMinAcceptAmount());
        if (dto.getMaxAcceptAmount() != null)       user.setMaxAcceptAmount(dto.getMaxAcceptAmount());
        if (dto.getDailyRecommendLimit() != null)   user.setDailyRecommendLimit(dto.getDailyRecommendLimit());
        user.setUpdatedAt(LocalDateTime.now(ZONE));
        userMapper.updateById(user);
        return toProfileVO(user);
    }

    // =========================================================
    // 接口实现：主推荐入口
    // =========================================================

    @Override
    public List<RecommendedTaskVO> recommendTasks(Long userId, Integer limit) {
        int realLimit = (limit != null && limit > 0) ? limit : DEFAULT_DAILY_LIMIT;
        User user = getUser(userId);
        if (blocked(userId)) return Collections.emptyList();
        List<String>  abilityTags        = parseList(user.getAbilityTags(),        new TypeReference<List<String>>()  {});
        List<Integer> preferredCategories = parseList(user.getPreferredCategories(), new TypeReference<List<Integer>>() {});
        if (abilityTags.isEmpty() && preferredCategories.isEmpty()) return coldStartRecommend(user, realLimit);
        Map<String, BigDecimal> weights       = parseWeights(user.getRecommendWeights());
        List<Task>              candidates    = fetchCandidates(userId);
        Map<Long, User>         publisherCache = batchLoadPublishers(candidates);
        Map<Integer, String>    categoryNames  = buildCategoryNameMap();
        return candidates.stream()
                .filter(t -> receivable(t, user))
                .map(t -> {
                    BigDecimal sAbility  = abilityScore(t, abilityTags);
                    BigDecimal sCategory = categoryScore(t, preferredCategories);
                    BigDecimal sAmount   = amountScore(t, user);
                    BigDecimal sDelivery = deliveryScore(t, user);
                    BigDecimal urgency   = urgencyScore(t);
                    BigDecimal credit    = creditBonusScore(t, publisherCache);
                    BigDecimal level     = levelMatchScore(t, user);
                    BigDecimal base = w(weights, "ability").multiply(sAbility)
                            .add(w(weights, "category").multiply(sCategory))
                            .add(w(weights, "amount").multiply(sAmount))
                            .add(w(weights, "delivery").multiply(sDelivery));
                    BigDecimal score = base.add(urgency).add(credit).add(level)
                            .setScale(4, RoundingMode.HALF_UP).min(BigDecimal.ONE);
                    RecommendedTaskVO vo = new RecommendedTaskVO();
                    vo.setTask(toCardVO(t, categoryNames, publisherCache));
                    vo.setScore(score);
                    vo.setRecommendReason(buildReason(sAbility, sCategory, sAmount, sDelivery, urgency, level, t));
                    Map<String, BigDecimal> bd = new LinkedHashMap<>();
                    bd.put("ability", sAbility); bd.put("category", sCategory);
                    bd.put("amount", sAmount);   bd.put("delivery", sDelivery);
                    bd.put("urgency", urgency);  bd.put("creditBonus", credit); bd.put("levelBonus", level);
                    vo.setScoreBreakdown(bd);
                    return vo;
                })
                .sorted(Comparator.comparing(RecommendedTaskVO::getScore).reversed())
                .limit(realLimit)
                .collect(Collectors.toList());
    }

    // =========================================================
    // 接口实现：结算后权重更新
    // =========================================================

    @Override
    @Transactional
    public void updateWeightsAfterSettlement(Long userId, Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) return;
        User user = getUser(userId);
        List<String>  abilityTags        = parseList(user.getAbilityTags(),        new TypeReference<List<String>>()  {});
        List<Integer> preferredCategories = parseList(user.getPreferredCategories(), new TypeReference<List<Integer>>() {});
        Map<String, BigDecimal> before   = parseWeights(user.getRecommendWeights());
        Map<String, BigDecimal> gradient = computeGradient(task, user, abilityTags, preferredCategories);
        Map<String, BigDecimal> updated  = new LinkedHashMap<>(before);
        for (String key : updated.keySet()) {
            BigDecimal g = gradient.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal newVal = updated.get(key).add(LEARNING_RATE.multiply(g));
            updated.put(key, newVal.max(WEIGHT_MIN).min(WEIGHT_MAX));
        }
        Map<String, BigDecimal> after = normalize(updated);
        user.setRecommendWeights(toJson(after));
        user.setUpdatedAt(LocalDateTime.now(ZONE));
        userMapper.updateById(user);
        logWeightChange(userId, taskId, before, after, 2, "结算后权重更新 taskId=" + taskId);
    }

    // =========================================================
    // 接口实现：点击反馈轻量级权重学习
    // =========================================================

    @Override
    @Transactional
    public void onTaskClick(Long userId, Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) return;
        User user = getUser(userId);
        List<String>  abilityTags        = parseList(user.getAbilityTags(),        new TypeReference<List<String>>()  {});
        List<Integer> preferredCategories = parseList(user.getPreferredCategories(), new TypeReference<List<Integer>>() {});
        Map<String, BigDecimal> before   = parseWeights(user.getRecommendWeights());
        BigDecimal clickLr = new BigDecimal("0.01");
        Map<String, BigDecimal> gradient = computeGradient(task, user, abilityTags, preferredCategories);
        Map<String, BigDecimal> updated  = new LinkedHashMap<>(before);
        for (String key : updated.keySet()) {
            BigDecimal g = gradient.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal newVal = updated.get(key).add(clickLr.multiply(g));
            updated.put(key, newVal.max(WEIGHT_MIN).min(WEIGHT_MAX));
        }
        Map<String, BigDecimal> after = normalize(updated);
        user.setRecommendWeights(toJson(after));
        user.setUpdatedAt(LocalDateTime.now(ZONE));
        userMapper.updateById(user);
        logWeightChange(userId, taskId, before, after, 3, "点击任务详情轻量反馈 taskId=" + taskId);
    }

    // =========================================================
    // 接口实现：协同过滤融合推荐
    // =========================================================

    @Override
    public List<RecommendedTaskVO> recommendTasksWithCF(Long userId, Integer limit) {
        int realLimit = (limit != null && limit > 0) ? limit : DEFAULT_DAILY_LIMIT;
        List<RecommendedTaskVO> mainList = recommendTasks(userId, realLimit * 2);
        List<RecommendedTaskVO> cfList = collaborativeFilterRecall(userId, realLimit);
        if (cfList.isEmpty()) return mainList.stream().limit(realLimit).collect(Collectors.toList());
        Map<Long, RecommendedTaskVO> merged = new LinkedHashMap<>();
        for (RecommendedTaskVO vo : mainList) merged.put(vo.getTask().getId(), vo);
        for (RecommendedTaskVO cfVo : cfList) {
            Long tid = cfVo.getTask().getId();
            if (merged.containsKey(tid)) {
                RecommendedTaskVO existing = merged.get(tid);
                BigDecimal fusedScore = existing.getScore().multiply(new BigDecimal("0.7"))
                        .add(cfVo.getScore().multiply(new BigDecimal("0.3"))).setScale(4, RoundingMode.HALF_UP);
                existing.setScore(fusedScore);
                if (!existing.getRecommendReason().contains("相似用户"))
                    existing.setRecommendReason(existing.getRecommendReason() + " · 相似用户也接过");
            } else {
                cfVo.setScore(cfVo.getScore().multiply(new BigDecimal("0.3")).setScale(4, RoundingMode.HALF_UP));
                merged.put(tid, cfVo);
            }
        }
        return merged.values().stream()
                .sorted(Comparator.comparing(RecommendedTaskVO::getScore).reversed())
                .limit(realLimit).collect(Collectors.toList());
    }

    private List<RecommendedTaskVO> collaborativeFilterRecall(Long userId, int limit) {
        Set<Long> myTaskIds = grabRecordMapper.selectList(
                new LambdaQueryWrapper<com.campus.task.module.task.entity.GrabRecord>()
                        .eq(com.campus.task.module.task.entity.GrabRecord::getUserId, userId)
        ).stream().map(com.campus.task.module.task.entity.GrabRecord::getTaskId).collect(Collectors.toSet());
        if (myTaskIds.isEmpty()) return Collections.emptyList();
        List<com.campus.task.module.task.entity.GrabRecord> allRecords = grabRecordMapper.selectList(
                new LambdaQueryWrapper<com.campus.task.module.task.entity.GrabRecord>()
                        .ne(com.campus.task.module.task.entity.GrabRecord::getUserId, userId)
                        .orderByDesc(com.campus.task.module.task.entity.GrabRecord::getCreatedAt)
                        .last("LIMIT 500")
        );
        if (allRecords.isEmpty()) return Collections.emptyList();
        Map<Long, Set<Long>> otherUserVectors = new LinkedHashMap<>();
        for (com.campus.task.module.task.entity.GrabRecord r : allRecords)
            otherUserVectors.computeIfAbsent(r.getUserId(), k -> new HashSet<>()).add(r.getTaskId());
        final int TOP_SIMILAR_USERS = 5;
        List<Map.Entry<Long, Double>> similarities = new ArrayList<>();
        for (Map.Entry<Long, Set<Long>> entry : otherUserVectors.entrySet()) {
            double sim = cosineSimilarity(myTaskIds, entry.getValue());
            if (sim > 0) similarities.add(Map.entry(entry.getKey(), sim));
        }
        similarities.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<Map.Entry<Long, Double>> topUsers = similarities.stream().limit(TOP_SIMILAR_USERS).collect(Collectors.toList());
        if (topUsers.isEmpty()) return Collections.emptyList();
        Map<Long, Double> taskCfScore = new LinkedHashMap<>();
        for (Map.Entry<Long, Double> simUser : topUsers) {
            Set<Long> theirTasks = otherUserVectors.get(simUser.getKey());
            for (Long tid : theirTasks)
                if (!myTaskIds.contains(tid)) taskCfScore.merge(tid, simUser.getValue(), Double::sum);
        }
        if (taskCfScore.isEmpty()) return Collections.emptyList();
        List<Long> candidateIds = taskCfScore.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit((long) limit * 2).map(Map.Entry::getKey).collect(Collectors.toList());
        List<Task> cfTasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>().in(Task::getId, candidateIds).eq(Task::getStatus, TaskStatus.PENDING_GRAB.getCode()));
        if (cfTasks.isEmpty()) return Collections.emptyList();
        User user = getUser(userId);
        Map<Long, User> publisherCache = batchLoadPublishers(cfTasks);
        Map<Integer, String> categoryNames = buildCategoryNameMap();
        double maxCfScore = taskCfScore.values().stream().mapToDouble(d -> d).max().orElse(1.0);
        return cfTasks.stream().filter(t -> receivable(t, user)).map(t -> {
            double rawCf = taskCfScore.getOrDefault(t.getId(), 0.0);
            BigDecimal cfScore = BigDecimal.valueOf(rawCf / maxCfScore).setScale(4, RoundingMode.HALF_UP).min(BigDecimal.ONE);
            RecommendedTaskVO vo = new RecommendedTaskVO();
            vo.setTask(toCardVO(t, categoryNames, publisherCache));
            vo.setScore(cfScore);
            vo.setRecommendReason("相似用户也接过这个任务");
            Map<String, BigDecimal> bd = new LinkedHashMap<>();
            bd.put("cfScore", cfScore);
            vo.setScoreBreakdown(bd);
            return vo;
        }).sorted(Comparator.comparing(RecommendedTaskVO::getScore).reversed()).limit(limit).collect(Collectors.toList());
    }

    private double cosineSimilarity(Set<Long> a, Set<Long> b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        long intersection = a.stream().filter(b::contains).count();
        if (intersection == 0) return 0.0;
        return intersection / (Math.sqrt(a.size()) * Math.sqrt(b.size()));
    }
}
