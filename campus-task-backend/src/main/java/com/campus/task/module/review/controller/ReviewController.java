package com.campus.task.module.review.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.task.common.enums.TaskStatus;
import com.campus.task.common.exception.BusinessException;
import com.campus.task.common.result.R;
import com.campus.task.common.utils.SnowflakeUtil;
import com.campus.task.module.review.entity.Review;
import com.campus.task.module.review.mapper.ReviewMapper;
import com.campus.task.module.review.vo.ReviewItemVO;
import com.campus.task.module.task.entity.Task;
import com.campus.task.module.task.mapper.TaskMapper;
import com.campus.task.module.user.entity.User;
import com.campus.task.module.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Tag(name = "评价模块")
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewMapper reviewMapper;
    private final TaskMapper taskMapper;
    private final UserMapper userMapper;
    private final SnowflakeUtil snowflakeUtil;

    @Operation(summary = "提交评价")
    @PostMapping("/submit")
    public R<Object> submit(@AuthenticationPrincipal UserDetails user,
                            @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(user.getUsername());
        Long taskId = Long.valueOf(body.get("taskId").toString());
        Integer score = Integer.valueOf(body.get("score").toString());
        String content = body.get("content").toString();

        if (score < 1 || score > 5) throw new BusinessException("星级范围1-5");
        if (content.length() < 1 || content.length() > 50) throw new BusinessException("评价内容1-50字");

        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException("任务不存在");
        if (task.getStatus() < TaskStatus.SETTLED.getCode()) throw new BusinessException("任务尚未结算");
        if (reviewMapper.existsReview(taskId, userId) > 0) throw new BusinessException(409, "已评价过该任务");

        boolean isPublisher = userId.equals(task.getPublisherId());
        boolean isAcceptor = userId.equals(task.getAcceptorId());
        if (!isPublisher && !isAcceptor) throw new BusinessException("无权评价");

        Long revieweeId = isPublisher ? task.getAcceptorId() : task.getPublisherId();
        Review review = new Review();
        review.setId(snowflakeUtil.nextId());
        review.setTaskId(taskId);
        review.setReviewerId(userId);
        review.setRevieweeId(revieweeId);
        review.setScore(score);
        review.setContent(content);
        review.setType(isPublisher ? 0 : 1);
        reviewMapper.insert(review);

        // 更新被评价人信誉分
        User reviewee = userMapper.selectById(revieweeId);
        int delta = score == 5 ? 2 : score == 4 ? 1 : score == 1 ? -1 : 0;
        if (delta != 0) {
            reviewee.setCreditScore(Math.max(0, reviewee.getCreditScore() + delta));
            userMapper.updateById(reviewee);
        }

        // 双方都评价后更新任务状态为已互评
        long reviewCount = reviewMapper.selectCount(
                new LambdaQueryWrapper<Review>().eq(Review::getTaskId, taskId));
        if (reviewCount >= 2) {
            task.setStatus(TaskStatus.REVIEWED.getCode());
            taskMapper.updateById(task);
        }

        return R.ok(Map.of("message", "评价成功", "newCreditScore", reviewee.getCreditScore()));
    }

    @Operation(summary = "查看用户收到的评价")
    @GetMapping("/user/{userId}")
    public R<Page<ReviewItemVO>> userReviews(@PathVariable Long userId,
                                              @RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Review> p = reviewMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getRevieweeId, userId)
                        .orderByDesc(Review::getCreatedAt));
        Page<ReviewItemVO> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        result.setRecords(p.getRecords().stream().map(this::toVO).toList());
        return R.ok(result);
    }

    @Operation(summary = "查看任务双方评价")
    @GetMapping("/task/{taskId}")
    public R<Object> taskReviews(@PathVariable Long taskId) {
        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>().eq(Review::getTaskId, taskId));
        ReviewItemVO pub = reviews.stream().filter(r -> r.getType() == 0)
                .findFirst().map(this::toVO).orElse(null);
        ReviewItemVO acc = reviews.stream().filter(r -> r.getType() == 1)
                .findFirst().map(this::toVO).orElse(null);
        return R.ok(Map.of(
                "publisherReview", pub != null ? pub : Map.of(),
                "acceptorReview", acc != null ? acc : Map.of()));
    }

    // ==================== 转换 ====================

    private ReviewItemVO toVO(Review review) {
        ReviewItemVO vo = new ReviewItemVO();
        vo.setId(review.getId());
        vo.setTaskId(review.getTaskId());
        // 查询任务标题
        Task task = taskMapper.selectById(review.getTaskId());
        vo.setTaskTitle(task != null ? task.getTitle() : "");
        // 查询评价人信息
        User reviewer = userMapper.selectById(review.getReviewerId());
        if (reviewer != null) {
            ReviewItemVO.ReviewerInfo info = new ReviewItemVO.ReviewerInfo();
            info.setId(reviewer.getId());
            info.setNickname(reviewer.getNickname());
            info.setAvatar(reviewer.getAvatar());
            vo.setReviewer(info);
        }
        vo.setScore(review.getScore());
        vo.setContent(review.getContent());
        vo.setType(review.getType());
        vo.setCreatedAt(review.getCreatedAt()
                .atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli());
        return vo;
    }
}
