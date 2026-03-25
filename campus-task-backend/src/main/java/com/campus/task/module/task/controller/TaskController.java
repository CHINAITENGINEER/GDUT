package com.campus.task.module.task.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.task.common.result.R;
import com.campus.task.module.task.dto.*;
import com.campus.task.module.task.service.TaskService;
import com.campus.task.module.task.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 任务接口
 */
@Tag(name = "任务模块")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "任务列表")
    @GetMapping
    public R<Page<TaskCardVO>> list(TaskQueryDTO dto) {
        return R.ok(taskService.list(dto));
    }

    @Operation(summary = "发布任务")
    @PostMapping
    public R<TaskDetailVO> publish(@AuthenticationPrincipal UserDetails user,
                                   @Valid @RequestBody TaskPublishDTO dto) {
        return R.ok(taskService.publish(Long.valueOf(user.getUsername()), dto));
    }

    @Operation(summary = "任务详情")
    @GetMapping("/{id}")
    public R<TaskDetailVO> detail(@PathVariable Long id,
                                   @AuthenticationPrincipal UserDetails user) {
        Long uid = user != null ? Long.valueOf(user.getUsername()) : null;
        return R.ok(taskService.detail(id, uid));
    }

    @Operation(summary = "抢单")
    @PostMapping("/{id}/grab")
    public R<GrabVO> grab(@PathVariable Long id,
                          @AuthenticationPrincipal UserDetails user) {
        return R.ok(taskService.grab(id, Long.valueOf(user.getUsername())));
    }

    @Operation(summary = "发布者确认接受抢单者")
    @PostMapping("/{id}/grab/confirm")
    public R<GrabConfirmVO> confirmGrab(@PathVariable Long id,
                                        @AuthenticationPrincipal UserDetails user) {
        return R.ok(taskService.confirmGrab(id, Long.valueOf(user.getUsername())));
    }

    @Operation(summary = "发布者拒绝抢单者")
    @PostMapping("/{id}/grab/reject")
    public R<Void> rejectGrab(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails user) {
        taskService.rejectGrab(id, Long.valueOf(user.getUsername()));
        return R.ok();
    }

    @Operation(summary = "抢单者主动取消")
    @PostMapping("/{id}/grab/cancel")
    public R<Void> cancelGrab(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails user) {
        taskService.cancelGrab(id, Long.valueOf(user.getUsername()));
        return R.ok();
    }

    @Operation(summary = "提交交付成果")
    @PostMapping("/{id}/submit")
    public R<Void> submit(@PathVariable Long id,
                          @AuthenticationPrincipal UserDetails user,
                          @Valid @RequestBody TaskSubmitDTO dto) {
        taskService.submit(id, Long.valueOf(user.getUsername()), dto);
        return R.ok();
    }

    @Operation(summary = "验收任务")
    @PostMapping("/{id}/verify")
    public R<VerifyVO> verify(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails user,
                              @Valid @RequestBody TaskVerifyDTO dto) {
        return R.ok(taskService.verify(id, Long.valueOf(user.getUsername()), dto));
    }

    @Operation(summary = "发布者取消任务")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id,
                          @AuthenticationPrincipal UserDetails user) {
        taskService.cancel(id, Long.valueOf(user.getUsername()));
        return R.ok();
    }

    @Operation(summary = "申请管理员介入")
    @PostMapping("/{id}/dispute")
    public R<Void> dispute(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails user,
                           @RequestBody Map<String, String> body) {
        taskService.dispute(id, Long.valueOf(user.getUsername()), body.get("reason"));
        return R.ok();
    }

    @Operation(summary = "我发布的任务")
    @GetMapping("/my/published")
    public R<Page<TaskCardVO>> myPublished(@AuthenticationPrincipal UserDetails user,
                                           @RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "10") Integer pageSize,
                                           @RequestParam(required = false) Integer status) {
        return R.ok(taskService.myPublished(Long.valueOf(user.getUsername()), page, pageSize, status));
    }

    @Operation(summary = "我抢单的任务")
    @GetMapping("/my/grabbed")
    public R<Page<TaskCardVO>> myGrabbed(@AuthenticationPrincipal UserDetails user,
                                         @RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                         @RequestParam(required = false) Integer status) {
        return R.ok(taskService.myGrabbed(Long.valueOf(user.getUsername()), page, pageSize, status));
    }
}
