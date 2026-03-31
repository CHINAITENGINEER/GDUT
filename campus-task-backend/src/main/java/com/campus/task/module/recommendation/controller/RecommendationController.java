package com.campus.task.module.recommendation.controller;

import com.campus.task.common.result.R;
import com.campus.task.module.recommendation.dto.RecommendationProfileDTO;
import com.campus.task.module.recommendation.service.RecommendationService;
import com.campus.task.module.recommendation.vo.RecommendationProfileVO;
import com.campus.task.module.recommendation.vo.RecommendedTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "自适应推荐Agent")
@RestController
@RequestMapping("/api/recommendation")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "获取我的推荐画像")
    @GetMapping("/profile")
    public R<RecommendationProfileVO> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return R.ok(recommendationService.getProfile(Long.valueOf(userDetails.getUsername())));
    }

    @Operation(summary = "保存我的推荐画像")
    @PutMapping("/profile")
    public R<RecommendationProfileVO> saveProfile(@AuthenticationPrincipal UserDetails userDetails,
                                                  @Valid @RequestBody RecommendationProfileDTO dto) {
        return R.ok(recommendationService.saveProfile(Long.valueOf(userDetails.getUsername()), dto));
    }

    @Operation(summary = "获取个性化推荐任务")
    @GetMapping("/tasks")
    public R<List<RecommendedTaskVO>> recommendTasks(@AuthenticationPrincipal UserDetails userDetails,
                                                     @RequestParam(defaultValue = "10") Integer limit) {
        return R.ok(recommendationService.recommendTasks(Long.valueOf(userDetails.getUsername()), limit));
    }

    @Operation(summary = "完成订单后手动触发权重更新")
    @PostMapping("/tasks/{taskId}/weights/refresh")
    public R<Void> refreshWeights(@AuthenticationPrincipal UserDetails userDetails,
                                  @PathVariable Long taskId) {
        recommendationService.updateWeightsAfterSettlement(Long.valueOf(userDetails.getUsername()), taskId);
        return R.ok();
    }

    @Operation(summary = "查看任务详情时触发轻量权重反馈")
    @PostMapping("/tasks/{taskId}/click")
    public R<Void> onTaskClick(@AuthenticationPrincipal UserDetails userDetails,
                               @PathVariable Long taskId) {
        recommendationService.onTaskClick(Long.valueOf(userDetails.getUsername()), taskId);
        return R.ok();
    }

    @Operation(summary = "融合协同过滤的个性化推荐")
    @GetMapping("/tasks/cf")
    public R<List<RecommendedTaskVO>> recommendTasksWithCF(@AuthenticationPrincipal UserDetails userDetails,
                                                           @RequestParam(defaultValue = "10") Integer limit) {
        return R.ok(recommendationService.recommendTasksWithCF(Long.valueOf(userDetails.getUsername()), limit));
    }
}
