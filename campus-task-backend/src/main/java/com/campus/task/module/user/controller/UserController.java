package com.campus.task.module.user.controller;

import com.campus.task.common.result.R;
import com.campus.task.module.user.dto.ChangePasswordDTO;
import com.campus.task.module.user.dto.UserUpdateDTO;
import com.campus.task.module.user.service.UserService;
import com.campus.task.module.user.vo.BalanceVO;
import com.campus.task.module.user.vo.LevelInfoVO;
import com.campus.task.module.user.vo.UserProfileVO;
import com.campus.task.module.user.vo.UserPublicVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户接口（需要Token）
 */
@Tag(name = "用户模块")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public R<Void> logout(@AuthenticationPrincipal UserDetails userDetails) {
        userService.logout(Long.valueOf(userDetails.getUsername()));
        return R.ok();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/profile")
    public R<UserProfileVO> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return R.ok(userService.getProfile(Long.valueOf(userDetails.getUsername())));
    }

    @Operation(summary = "修改个人资料")
    @PutMapping("/profile")
    public R<UserProfileVO> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                           @Valid @RequestBody UserUpdateDTO dto) {
        return R.ok(userService.updateProfile(Long.valueOf(userDetails.getUsername()), dto));
    }

    @Operation(summary = "切换角色")
    @PutMapping("/switch-role")
    public R<Map<String, String>> switchRole(@AuthenticationPrincipal UserDetails userDetails,
                                              @RequestBody Map<String, String> body) {
        String role = userService.switchRole(Long.valueOf(userDetails.getUsername()), body.get("role"));
        return R.ok(Map.of("currentRole", role));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public R<Void> changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                   @Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(Long.valueOf(userDetails.getUsername()), dto);
        return R.ok();
    }

    @Operation(summary = "查看等级详情")
    @GetMapping("/level-info")
    public R<LevelInfoVO> getLevelInfo(@AuthenticationPrincipal UserDetails userDetails) {
        return R.ok(userService.getLevelInfo(Long.valueOf(userDetails.getUsername())));
    }

    @Operation(summary = "查看账户余额")
    @GetMapping("/balance")
    public R<BalanceVO> getBalance(@AuthenticationPrincipal UserDetails userDetails) {
        return R.ok(userService.getBalance(Long.valueOf(userDetails.getUsername())));
    }

    @Operation(summary = "查看他人主页")
    @GetMapping("/{id}")
    public R<UserPublicVO> getPublicProfile(@PathVariable Long id) {
        return R.ok(userService.getPublicProfile(id));
    }
}
