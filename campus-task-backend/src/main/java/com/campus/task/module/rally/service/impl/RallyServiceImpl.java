package com.campus.task.module.rally.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.task.common.exception.BusinessException;
import com.campus.task.common.utils.SnowflakeUtil;
import com.campus.task.module.rally.dto.RallyCreateDTO;
import com.campus.task.module.rally.entity.RallyActivity;
import com.campus.task.module.rally.entity.RallyMember;
import com.campus.task.module.rally.entity.RallyMessage;
import com.campus.task.module.rally.handler.RallyLobbyWebSocketHandler;
import com.campus.task.module.rally.handler.RallyWebSocketHandler;
import com.campus.task.module.rally.mapper.RallyActivityMapper;
import com.campus.task.module.rally.mapper.RallyMemberMapper;
import com.campus.task.module.rally.mapper.RallyMessageMapper;
import com.campus.task.module.rally.service.RallyService;
import com.campus.task.module.rally.vo.RallyActivityVO;
import com.campus.task.module.rally.vo.RallyMemberVO;
import com.campus.task.module.rally.vo.RallyMessageVO;
import com.campus.task.module.user.entity.User;
import com.campus.task.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RallyServiceImpl implements RallyService {

    private final RallyActivityMapper rallyActivityMapper;
    private final RallyMemberMapper rallyMemberMapper;
    private final RallyMessageMapper rallyMessageMapper;
    private final UserMapper userMapper;
    private final SnowflakeUtil snowflakeUtil;
    private final RallyLobbyWebSocketHandler rallyLobbyWebSocketHandler;
    private final RallyWebSocketHandler rallyWebSocketHandler;

    @Override
    @Transactional
    public RallyActivityVO create(Long organizerId, RallyCreateDTO dto) {
        RallyActivity activity = new RallyActivity();
        activity.setId(snowflakeUtil.nextId());
        activity.setOrganizerId(organizerId);
        activity.setType(dto.getType());
        activity.setTitle(dto.getTitle());
        activity.setRecruitCount(dto.getRecruitCount());
        activity.setCurrentCount(1);
        activity.setStartTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(dto.getStartTime()), ZoneId.of("Asia/Shanghai")));
        activity.setRemark(StringUtils.hasText(dto.getRemark()) ? dto.getRemark().trim() : null);
        activity.setStatus(0);
        rallyActivityMapper.insert(activity);

        RallyMember organizer = new RallyMember();
        organizer.setId(snowflakeUtil.nextId());
        organizer.setRallyId(activity.getId());
        organizer.setUserId(organizerId);
        organizer.setRole(0);
        organizer.setStatus(1);
        organizer.setJoinedAt(LocalDateTime.now());
        rallyMemberMapper.insert(organizer);

        RallyActivityVO vo = buildActivityVO(activity, true);
        rallyLobbyWebSocketHandler.broadcast(Map.of("type", "rally_created", "activity", vo));
        return vo;
    }

    @Override
    public List<RallyActivityVO> listActive() {
        List<RallyActivity> activities = rallyActivityMapper.selectList(new LambdaQueryWrapper<RallyActivity>()
                .eq(RallyActivity::getStatus, 0)
                .orderByDesc(RallyActivity::getCreatedAt));
        List<RallyActivityVO> result = new ArrayList<>();
        for (RallyActivity a : activities) result.add(buildActivityVO(a, false));
        return result;
    }

    @Override
    @Transactional
    public RallyActivityVO join(Long rallyId, Long userId) {
        RallyActivity activity = getActiveActivity(rallyId);
        RallyMember existed = rallyMemberMapper.selectOne(new LambdaQueryWrapper<RallyMember>()
                .eq(RallyMember::getRallyId, rallyId)
                .eq(RallyMember::getUserId, userId)
                .last("LIMIT 1"));
        if (existed != null && existed.getStatus() == 1) throw new BusinessException("你已加入该活动");
        if (activity.getCurrentCount() >= activity.getRecruitCount()) throw new BusinessException("活动人数已满");

        if (existed == null) {
            RallyMember m = new RallyMember();
            m.setId(snowflakeUtil.nextId());
            m.setRallyId(rallyId);
            m.setUserId(userId);
            m.setRole(1);
            m.setStatus(1);
            m.setJoinedAt(LocalDateTime.now());
            rallyMemberMapper.insert(m);
        } else {
            existed.setStatus(1);
            if (existed.getRole() == null) existed.setRole(1);
            existed.setJoinedAt(LocalDateTime.now());
            existed.setQuitAt(null);
            rallyMemberMapper.updateById(existed);
        }

        activity.setCurrentCount(activity.getCurrentCount() + 1);
        rallyActivityMapper.updateById(activity);

        User user = userMapper.selectById(userId);
        Map<String, Object> roomEvent = new HashMap<>();
        roomEvent.put("type", "member_joined");
        roomEvent.put("rallyId", String.valueOf(rallyId));
        roomEvent.put("userId", String.valueOf(userId));
        roomEvent.put("nickname", user != null ? user.getNickname() : "用户");
        roomEvent.put("avatar", user != null && user.getAvatar() != null ? user.getAvatar() : "");
        roomEvent.put("currentCount", activity.getCurrentCount());
        rallyWebSocketHandler.broadcastToRoom(rallyId, roomEvent);

        rallyLobbyWebSocketHandler.broadcast(Map.of("type", "rally_updated", "activity", buildActivityVO(activity, false)));
        return buildActivityVO(activity, true);
    }

    @Override
    @Transactional
    public RallyActivityVO quit(Long rallyId, Long userId) {
        RallyActivity activity = getActiveActivity(rallyId);
        if (userId.equals(activity.getOrganizerId())) throw new BusinessException("发起人不能退出，请直接结束活动");

        RallyMember member = rallyMemberMapper.selectOne(new LambdaQueryWrapper<RallyMember>()
                .eq(RallyMember::getRallyId, rallyId)
                .eq(RallyMember::getUserId, userId)
                .eq(RallyMember::getStatus, 1)
                .last("LIMIT 1"));
        if (member == null) throw new BusinessException("你未加入该活动");

        member.setStatus(0);
        member.setQuitAt(LocalDateTime.now());
        rallyMemberMapper.updateById(member);

        activity.setCurrentCount(Math.max(1, activity.getCurrentCount() - 1));
        rallyActivityMapper.updateById(activity);

        User user = userMapper.selectById(userId);
        Map<String, Object> roomEvent = new HashMap<>();
        roomEvent.put("type", "member_quit");
        roomEvent.put("rallyId", String.valueOf(rallyId));
        roomEvent.put("userId", String.valueOf(userId));
        roomEvent.put("nickname", user != null ? user.getNickname() : "用户");
        roomEvent.put("currentCount", activity.getCurrentCount());
        rallyWebSocketHandler.broadcastToRoom(rallyId, roomEvent);

        rallyLobbyWebSocketHandler.broadcast(Map.of("type", "rally_updated", "activity", buildActivityVO(activity, false)));
        return buildActivityVO(activity, true);
    }

    @Override
    @Transactional
    public void end(Long rallyId, Long userId) {
        RallyActivity activity = rallyActivityMapper.selectById(rallyId);
        if (activity == null) throw new BusinessException("活动不存在");
        if (!userId.equals(activity.getOrganizerId())) throw new BusinessException(403, "仅发起人可结束活动");
        if (activity.getStatus() == 1) return;

        activity.setStatus(1);
        activity.setEndedAt(LocalDateTime.now());
        rallyActivityMapper.updateById(activity);

        rallyWebSocketHandler.broadcastToRoom(rallyId, Map.of("type", "rally_ended", "rallyId", String.valueOf(rallyId)));
        rallyLobbyWebSocketHandler.broadcast(Map.of("type", "rally_ended", "rallyId", String.valueOf(rallyId)));
    }

    @Override
    public List<RallyMemberVO> members(Long rallyId, Long userId) {
        assertJoined(rallyId, userId);
        return listActiveMembers(rallyId);
    }

    @Override
    public List<RallyMessageVO> history(Long rallyId, Long userId) {
        assertJoined(rallyId, userId);
        List<RallyMessage> messages = rallyMessageMapper.selectList(new LambdaQueryWrapper<RallyMessage>()
                .eq(RallyMessage::getRallyId, rallyId)
                .orderByAsc(RallyMessage::getCreatedAt)
                .last("LIMIT 200"));
        List<RallyMessageVO> result = new ArrayList<>();
        for (RallyMessage msg : messages) {
            User sender = userMapper.selectById(msg.getSenderId());
            RallyMessageVO vo = new RallyMessageVO();
            vo.setId(msg.getId());
            vo.setRallyId(msg.getRallyId());
            vo.setSenderId(msg.getSenderId());
            vo.setSenderNickname(sender != null ? sender.getNickname() : "用户");
            vo.setSenderAvatar(sender != null && sender.getAvatar() != null ? sender.getAvatar() : "");
            vo.setContent(msg.getContent());
            vo.setCreatedAt(msg.getCreatedAt() != null ? msg.getCreatedAt().atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli() : 0L);
            result.add(vo);
        }
        return result;
    }

    private RallyActivity getActiveActivity(Long rallyId) {
        RallyActivity a = rallyActivityMapper.selectById(rallyId);
        if (a == null) throw new BusinessException("活动不存在");
        if (a.getStatus() != 0) throw new BusinessException("活动已结束");
        return a;
    }

    private void assertJoined(Long rallyId, Long userId) {
        RallyActivity activity = rallyActivityMapper.selectById(rallyId);
        if (activity == null) throw new BusinessException("活动不存在");
        if (activity.getStatus() == 1) throw new BusinessException("活动已结束");
        RallyMember m = rallyMemberMapper.selectOne(new LambdaQueryWrapper<RallyMember>()
                .eq(RallyMember::getRallyId, rallyId)
                .eq(RallyMember::getUserId, userId)
                .eq(RallyMember::getStatus, 1)
                .last("LIMIT 1"));
        if (m == null) throw new BusinessException(403, "无权访问该活动");
    }

    private RallyActivityVO buildActivityVO(RallyActivity a, boolean includeMembers) {
        User organizer = userMapper.selectById(a.getOrganizerId());
        RallyActivityVO vo = new RallyActivityVO();
        vo.setId(a.getId());
        vo.setType(a.getType());
        vo.setTypeName(a.getType() != null && a.getType() == 1 ? "运动" : "游戏");
        vo.setTitle(a.getTitle());
        vo.setRecruitCount(a.getRecruitCount());
        vo.setCurrentCount(a.getCurrentCount());
        vo.setStartTime(toTs(a.getStartTime()));
        vo.setRemark(a.getRemark());
        vo.setStatus(a.getStatus());
        vo.setOrganizerId(a.getOrganizerId());
        vo.setOrganizerNickname(organizer != null ? organizer.getNickname() : "用户");
        vo.setOrganizerAvatar(organizer != null && organizer.getAvatar() != null ? organizer.getAvatar() : "");
        vo.setCreatedAt(toTs(a.getCreatedAt()));
        if (includeMembers) vo.setMembers(listActiveMembers(a.getId()));
        return vo;
    }

    private List<RallyMemberVO> listActiveMembers(Long rallyId) {
        List<RallyMember> members = rallyMemberMapper.selectList(new LambdaQueryWrapper<RallyMember>()
                .eq(RallyMember::getRallyId, rallyId)
                .eq(RallyMember::getStatus, 1)
                .orderByAsc(RallyMember::getRole)
                .orderByAsc(RallyMember::getJoinedAt));
        List<RallyMemberVO> result = new ArrayList<>();
        for (RallyMember m : members) {
            User u = userMapper.selectById(m.getUserId());
            RallyMemberVO vo = new RallyMemberVO();
            vo.setUserId(m.getUserId());
            vo.setNickname(u != null ? u.getNickname() : "用户");
            vo.setAvatar(u != null && u.getAvatar() != null ? u.getAvatar() : "");
            vo.setRole(m.getRole());
            vo.setJoinedAt(toTs(m.getJoinedAt()));
            result.add(vo);
        }
        return result;
    }

    private Long toTs(LocalDateTime time) {
        return time == null ? null : time.atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
    }
}
