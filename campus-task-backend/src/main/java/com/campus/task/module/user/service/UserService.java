package com.campus.task.module.user.service;

import com.campus.task.module.user.dto.*;
import com.campus.task.module.user.vo.BalanceVO;
import com.campus.task.module.user.vo.LevelInfoVO;
import com.campus.task.module.user.vo.LoginVO;
import com.campus.task.module.user.vo.UserProfileVO;
import com.campus.task.module.user.vo.UserPublicVO;

/**
 * 用户业务接口
 */
public interface UserService {

    /** 发送短信验证码 */
    void sendSmsCode(SmsSendDTO dto);

    /** 用户注册 */
    LoginVO register(RegisterDTO dto);

    /** 用户登录 */
    LoginVO login(LoginDTO dto);

    /** 登出 */
    void logout(Long userId);

    /** 获取当前用户信息 */
    UserProfileVO getProfile(Long userId);

    /** 修改个人资料 */
    UserProfileVO updateProfile(Long userId, UserUpdateDTO dto);

    /** 切换角色 */
    String switchRole(Long userId, String role);

    /** 修改密码 */
    void changePassword(Long userId, ChangePasswordDTO dto);

    /** 重置密码 */
    void resetPassword(String phone, String smsCode, String newPassword);

    /** 查看他人主页 */
    UserPublicVO getPublicProfile(Long targetUserId);

    /** 查看等级详情 */
    LevelInfoVO getLevelInfo(Long userId);

    /** 查看账户余额 */
    BalanceVO getBalance(Long userId);
}
