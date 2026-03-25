package com.campus.task.module.user.controller;

import com.campus.task.common.result.R;
import com.campus.task.module.user.dto.*;
import com.campus.task.module.user.service.UserService;
import com.campus.task.module.user.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口（无需Token）
 */
@Tag(name = "认证模块")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "发送短信验证码")
    @PostMapping("/sms/send")
    public R<Void> sendSmsCode(@Valid @RequestBody SmsSendDTO dto) {
        userService.sendSmsCode(dto);
        return R.ok();
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        return R.ok(userService.register(dto));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(userService.login(dto));
    }

    @Operation(summary = "重置密码")
    @PostMapping("/password/reset")
    public R<Void> resetPassword(@RequestBody ResetPasswordDTO dto) {
        userService.resetPassword(dto.getPhone(), dto.getSmsCode(), dto.getNewPassword());
        return R.ok();
    }
}
