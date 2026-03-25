package com.campus.task.module.rally.controller;

import com.campus.task.common.result.R;
import com.campus.task.module.rally.dto.RallyCreateDTO;
import com.campus.task.module.rally.service.RallyService;
import com.campus.task.module.rally.vo.RallyActivityVO;
import com.campus.task.module.rally.vo.RallyMemberVO;
import com.campus.task.module.rally.vo.RallyMessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "召集组队")
@RestController
@RequestMapping("/api/rally")
@RequiredArgsConstructor
public class RallyController {

    private final RallyService rallyService;

    @Operation(summary = "发起召集活动")
    @PostMapping
    public R<RallyActivityVO> create(@AuthenticationPrincipal UserDetails user,
                                     @Valid @RequestBody RallyCreateDTO dto) {
        return R.ok(rallyService.create(Long.valueOf(user.getUsername()), dto));
    }

    @Operation(summary = "活动列表（未结束）")
    @GetMapping("/list")
    public R<List<RallyActivityVO>> listActive() {
        return R.ok(rallyService.listActive());
    }

    @Operation(summary = "参加活动")
    @PostMapping("/{id}/join")
    public R<RallyActivityVO> join(@PathVariable Long id,
                                   @AuthenticationPrincipal UserDetails user) {
        return R.ok(rallyService.join(id, Long.valueOf(user.getUsername())));
    }

    @Operation(summary = "退出活动")
    @PostMapping("/{id}/quit")
    public R<RallyActivityVO> quit(@PathVariable Long id,
                                   @AuthenticationPrincipal UserDetails user) {
        return R.ok(rallyService.quit(id, Long.valueOf(user.getUsername())));
    }

    @Operation(summary = "结束活动（仅发起人）")
    @PostMapping("/{id}/end")
    public R<Void> end(@PathVariable Long id,
                       @AuthenticationPrincipal UserDetails user) {
        rallyService.end(id, Long.valueOf(user.getUsername()));
        return R.ok();
    }

    @Operation(summary = "活动成员列表")
    @GetMapping("/{id}/members")
    public R<List<RallyMemberVO>> members(@PathVariable Long id,
                                          @AuthenticationPrincipal UserDetails user) {
        return R.ok(rallyService.members(id, Long.valueOf(user.getUsername())));
    }

    @Operation(summary = "活动聊天历史")
    @GetMapping("/{id}/history")
    public R<List<RallyMessageVO>> history(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetails user) {
        return R.ok(rallyService.history(id, Long.valueOf(user.getUsername())));
    }
}
