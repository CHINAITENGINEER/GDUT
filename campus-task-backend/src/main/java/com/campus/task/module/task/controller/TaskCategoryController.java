package com.campus.task.module.task.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.task.common.result.R;
import com.campus.task.module.task.entity.TaskCategory;
import com.campus.task.module.task.mapper.TaskCategoryMapper;
import com.campus.task.module.task.vo.TaskCategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开分类接口（无需Token，供前端渲染标签栏）
 */
@Tag(name = "任务分类")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class TaskCategoryController {

    private final TaskCategoryMapper taskCategoryMapper;
    private final StringRedisTemplate redisTemplate;

    @Operation(summary = "获取所有启用的分类标签")
    @GetMapping
    public R<List<TaskCategoryVO>> list() {
        List<TaskCategory> categories = taskCategoryMapper.selectList(
                new LambdaQueryWrapper<TaskCategory>()
                        .eq(TaskCategory::getIsActive, 1)
                        .orderByAsc(TaskCategory::getSort));
        List<TaskCategoryVO> vos = categories.stream().map(c -> {
            TaskCategoryVO vo = new TaskCategoryVO();
            vo.setId(c.getId());
            vo.setName(c.getName());
            vo.setIcon(c.getIcon());
            vo.setSort(c.getSort());
            vo.setActive(c.getIsActive() == 1);
            return vo;
        }).toList();
        return R.ok(vos);
    }
}
