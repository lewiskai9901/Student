package com.school.management.service;

import com.school.management.dto.*;

/**
 * 认证服务接口
 *
 * @author system
 * @since 1.0.0
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param request  登录请求
     * @param clientIp 客户端IP
     * @return 登录响应
     */
    LoginResponse login(LoginRequest request, String clientIp);

    /**
     * 刷新令牌
     *
     * @param request 刷新令牌请求
     * @return 登录响应
     */
    LoginResponse refreshToken(RefreshTokenRequest request);

    /**
     * 用户退出登录
     *
     * @param accessToken 访问令牌
     * @param request     退出请求
     */
    void logout(String accessToken, LogoutRequest request);

    /**
     * 获取当前用户信息
     *
     * @return 用户信息
     */
    LoginResponse.UserInfo getCurrentUserInfo();

    /**
     * 微信小程序登录
     *
     * @param request  微信登录请求
     * @param clientIp 客户端IP
     * @return 微信登录响应
     */
    WxLoginResponse wxLogin(WxLoginRequest request, String clientIp);

    /**
     * 微信账号绑定
     *
     * @param request  绑定请求
     * @param clientIp 客户端IP
     * @return 登录响应
     */
    LoginResponse wxBind(WxBindRequest request, String clientIp);

    /**
     * 检查微信是否已绑定
     *
     * @param openId 微信OpenID
     * @return 是否已绑定
     */
    boolean checkWxBinding(String openId);

    /**
     * 更新个人资料
     *
     * @param request 个人资料更新请求
     */
    void updateProfile(ProfileUpdateRequest request);

    /**
     * 修改密码
     *
     * @param request 修改密码请求
     */
    void changePassword(ChangePasswordRequest request);
}