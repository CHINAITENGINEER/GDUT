package com.campus.task.module.recommendation.service;

import com.campus.task.module.recommendation.dto.RecommendationProfileDTO;
import com.campus.task.module.recommendation.vo.RecommendationProfileVO;
import com.campus.task.module.recommendation.vo.RecommendedTaskVO;

import java.util.List;

public interface RecommendationService {

    RecommendationProfileVO getProfile(Long userId);

    RecommendationProfileVO saveProfile(Long userId, RecommendationProfileDTO dto);

    List<RecommendedTaskVO> recommendTasks(Long userId, Integer limit);

    void updateWeightsAfterSettlement(Long userId, Long taskId);

    /**
     * 用户点击任务详情时触发的轻量级权重反馈更新。
     * 学习率为 0.01（结算后为 0.04），梯度规则与结算后一致但更温和，
     * 不记录日志，不影响已有逻辑。
     *
     * @param userId 当前登录用户 ID
     * @param taskId 被点击查看的任务 ID
     */
    void onTaskClick(Long userId, Long taskId);

    /**
     * 融合协同过滤的个性化推荐。
     * 在原有线性加权推荐结果基础上，混入协同过滤召回的任务，
     * 按融合分数重新排序后返回 Top-K。
     *
     * @param userId 当前登录用户 ID
     * @param limit  返回数量上限
     * @return 融合后的推荐任务列表
     */
    List<RecommendedTaskVO> recommendTasksWithCF(Long userId, Integer limit);
}
