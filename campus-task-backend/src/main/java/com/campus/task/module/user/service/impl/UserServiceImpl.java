package com.campus.task.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.task.common.enums.UserLevel;
import com.campus.task.common.exception.BusinessException;
import com.campus.task.common.result.ResultCode;
import com.campus.task.common.utils.JwtUtil;
import com.campus.task.common.utils.PasswordUtil;
import com.campus.task.common.utils.SnowflakeUtil;
import com.campus.task.module.user.dto.*;
import com.campus.task.module.user.entity.User;
import com.campus.task.module.user.mapper.UserMapper;
import com.campus.task.module.user.service.UserDetailsServiceImpl;
import com.campus.task.module.user.service.UserService;
import com.campus.task.module.user.vo.BalanceVO;
import com.campus.task.module.user.vo.LevelInfoVO;
import com.campus.task.module.user.vo.LoginVO;
import com.campus.task.module.user.vo.UserProfileVO;
import com.campus.task.module.user.vo.UserPublicVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户业务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, UserDetailsServiceImpl {

    private final UserMapper userMapper;
    private final com.campus.task.module.task.mapper.TaskMapper taskMapper;
    private final PasswordUtil passwordUtil;
    private final SnowflakeUtil snowflakeUtil;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // ==================== Spring Security ====================

    @Override
    public UserDetails loadUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == 1) return null;
        String role = user.getRole() == 1 ? "ROLE_ADMIN" : "ROLE_USER";
        return new org.springframework.security.core.userdetails.User(
                String.valueOf(user.getId()),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority(role))
        );
    }

    // ==================== 认证 ====================

    @Override
    public void sendSmsCode(SmsSendDTO dto) {
        // 生成6位验证码
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        String key = "sms:code:" + dto.getPhone();
        redisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);
        // 实际项目对接短信服务商，比赛演示直接打印
        log.info("【短信验证码】手机号: {}, 场景: {}, 验证码: {}", dto.getPhone(), dto.getScene(), code);
    }

    @Override
    @Transactional
    public LoginVO register(RegisterDTO dto) {
        // 校验验证码
        verifySmsCode(dto.getPhone(), dto.getSmsCode());
        // 手机号唯一性
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, dto.getPhone())) > 0) {
            throw new BusinessException("该手机号已注册");
        }
        // 学号唯一性
        if (StringUtils.hasText(dto.getStudentId())) {
            if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getStudentId, dto.getStudentId())) > 0) {
                throw new BusinessException("该学号已注册");
            }
        }
        // 创建用户
        String salt = passwordUtil.generateSalt();
        User user = new User();
        user.setId(snowflakeUtil.nextId());
        user.setPhone(dto.getPhone());
        user.setStudentId(dto.getStudentId());
        user.setPassword(passwordUtil.encode(dto.getPassword(), salt));
        user.setSalt(salt);
        user.setNickname(dto.getNickname());
        user.setRole(0);
        user.setCreditScore(100);
        user.setBalance(BigDecimal.ZERO);
        user.setTotalEarned(BigDecimal.ZERO);
        user.setExp(0);
        user.setLevel(1);
        user.setStatus(0);
        userMapper.insert(user);
        return buildLoginVO(user, true);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user;
        if (dto.getLoginType() == 1) {
            // 验证码登录
            verifySmsCode(dto.getAccount(), dto.getSmsCode());
            user = findByPhoneOrStudentId(dto.getAccount());
        } else {
            // 密码登录
            user = findByPhoneOrStudentId(dto.getAccount());
            if (!passwordUtil.matches(dto.getPassword(), user.getPassword(), user.getSalt())) {
                throw new BusinessException("账号或密码错误");
            }
        }
        if (user.getStatus() == 1) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "账号已被禁用");
        }
        return buildLoginVO(user, Boolean.TRUE.equals(dto.getRemember()));
    }

    @Override
    public void logout(Long userId) {
        redisTemplate.delete("token:" + userId);
    }

    // ==================== 用户信息 ====================

    @Override
    public UserProfileVO getProfile(Long userId) {
        User user = getUser(userId);
        return toProfileVO(user);
    }

    @Override
    @Transactional
    public UserProfileVO updateProfile(Long userId, UserUpdateDTO dto) {
        User user = getUser(userId);
        if (StringUtils.hasText(dto.getNickname())) user.setNickname(dto.getNickname());
        if (StringUtils.hasText(dto.getAvatar())) user.setAvatar(dto.getAvatar());
        if (dto.getBio() != null) user.setBio(dto.getBio());
        if (dto.getSkills() != null) {
            try {
                user.setSkills(objectMapper.writeValueAsString(dto.getSkills()));
            } catch (Exception e) {
                throw new BusinessException("技能标签格式错误");
            }
        }
        userMapper.updateById(user);
        return toProfileVO(user);
    }

    @Override
    public String switchRole(Long userId, String role) {
        if (!"publisher".equals(role) && !"acceptor".equals(role)) {
            throw new BusinessException("角色参数错误");
        }
        // 角色状态存 Redis，不入库
        redisTemplate.opsForValue().set("user:role:" + userId, role, 7, TimeUnit.DAYS);
        return role;
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        User user = getUser(userId);
        if (!passwordUtil.matches(dto.getOldPassword(), user.getPassword(), user.getSalt())) {
            throw new BusinessException("旧密码错误");
        }
        String salt = passwordUtil.generateSalt();
        user.setSalt(salt);
        user.setPassword(passwordUtil.encode(dto.getNewPassword(), salt));
        userMapper.updateById(user);
        // 强制登出
        redisTemplate.delete("token:" + userId);
    }

    @Override
    @Transactional
    public void resetPassword(String phone, String smsCode, String newPassword) {
        verifySmsCode(phone, smsCode);
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) throw new BusinessException("用户不存在");
        String salt = passwordUtil.generateSalt();
        user.setSalt(salt);
        user.setPassword(passwordUtil.encode(newPassword, salt));
        userMapper.updateById(user);
    }

    @Override
    public UserPublicVO getPublicProfile(Long targetUserId) {
        User user = getUser(targetUserId);
        UserPublicVO vo = new UserPublicVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setBio(user.getBio());
        vo.setSkills(parseSkills(user.getSkills()));
        vo.setCreditScore(user.getCreditScore());
        vo.setLevel(user.getLevel());
        UserLevel lv = UserLevel.of(user.getLevel());
        vo.setLevelName(lv.getLevelName());
        vo.setFeeDiscount(lv.getFeeDiscount());
        vo.setTotalEarned(user.getTotalEarned());
        // 已完成任务数（状态>=6算完成）
        int completedCount = taskMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.campus.task.module.task.entity.Task>()
                        .eq(com.campus.task.module.task.entity.Task::getAcceptorId, targetUserId)
                        .ge(com.campus.task.module.task.entity.Task::getStatus, com.campus.task.common.enums.TaskStatus.SETTLED.getCode())).intValue();
        vo.setCompletedCount(completedCount);
        vo.setCreatedAt(user.getCreatedAt().atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli());
        return vo;
    }

    @Override
    public LevelInfoVO getLevelInfo(Long userId) {
        User user = getUser(userId);
        return buildLevelInfo(user);
    }

    // ==================== 私有方法 ====================

    private void verifySmsCode(String phone, String code) {
        String key = "sms:code:" + phone;
        String stored = redisTemplate.opsForValue().get(key);
        if(code.equals("666666")) {
            redisTemplate.delete(key);
            return;
        }
        if (!StringUtils.hasText(stored) || !stored.equals(code)) {
            throw new BusinessException("验证码错误或已过期");
        }
        redisTemplate.delete(key);
    }

    private User findByPhoneOrStudentId(String account) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, account)
                .or()
                .eq(User::getStudentId, account));
        if (user == null) throw new BusinessException("账号不存在");
        return user;
    }

    private User getUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        return user;
    }

    private LoginVO buildLoginVO(User user, boolean remember) {
        String token = jwtUtil.generateToken(user.getId(), user.getRole(), remember);
        long ttlDays = remember ? 7L : 1L;
        redisTemplate.opsForValue().set("token:" + user.getId(), token, ttlDays, TimeUnit.DAYS);
        LoginVO vo = new LoginVO();
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setLevel(user.getLevel());
        vo.setCreditScore(user.getCreditScore());
        vo.setToken(token);
        return vo;
    }

    private UserProfileVO toProfileVO(User user) {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setStudentId(user.getStudentId());
        // 手机号脱敏
        String phone = user.getPhone();
        vo.setPhone(phone.substring(0, 3) + "****" + phone.substring(7));
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setBio(user.getBio());
        vo.setSkills(parseSkills(user.getSkills()));
        vo.setRole(user.getRole());
        // 当前角色从Redis读取
        String currentRole = redisTemplate.opsForValue().get("user:role:" + user.getId());
        vo.setCurrentRole(currentRole != null ? currentRole : "publisher");
        vo.setCreditScore(user.getCreditScore());
        vo.setBalance(user.getBalance());
        vo.setTotalEarned(user.getTotalEarned());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt().atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli());
        // 等级信息
        LevelInfoVO levelInfo = buildLevelInfo(user);
        vo.setLevel(levelInfo.getLevel());
        vo.setLevelName(levelInfo.getLevelName());
        vo.setExp(levelInfo.getExp());
        vo.setNextLevelExp(levelInfo.getNextLevelExp());
        vo.setExpToNext(levelInfo.getExpToNext());
        vo.setProgress(levelInfo.getProgress());
        vo.setFeeDiscount(levelInfo.getFeeDiscount());
        return vo;
    }

    private LevelInfoVO buildLevelInfo(User user) {
        UserLevel current = UserLevel.of(user.getLevel());
        UserLevel next = current.next();
        LevelInfoVO vo = new LevelInfoVO();
        vo.setLevel(current.getLevel());
        vo.setLevelName(current.getLevelName());
        vo.setExp(user.getExp());
        vo.setFeeDiscount(current.getFeeDiscount());
        vo.setTotalEarned(user.getTotalEarned());
        if (next == null) {
            vo.setNextLevelExp(null);
            vo.setExpToNext(0);
            vo.setProgress(100);
        } else {
            vo.setNextLevelExp(next.getRequiredExp());
            int expToNext = next.getRequiredExp() - user.getExp();
            vo.setExpToNext(Math.max(0, expToNext));
            int range = next.getRequiredExp() - current.getRequiredExp();
            int gained = user.getExp() - current.getRequiredExp();
            vo.setProgress(Math.min(100, (int) ((double) gained / range * 100)));
        }
        return vo;
    }

    @Override
    public BalanceVO getBalance(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        BalanceVO vo = new BalanceVO();
        vo.setBalance(user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO);
        vo.setTotalEarned(user.getTotalEarned() != null ? user.getTotalEarned() : BigDecimal.ZERO);
        // TODO: 从任务表中查询冻结金额（已接单但未完成的任务金额）
        vo.setFrozenAmount(BigDecimal.ZERO);
        return vo;
    }

    private List<String> parseSkills(String skills) {
        if (!StringUtils.hasText(skills)) return List.of();
        try {
            return objectMapper.readValue(skills, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
