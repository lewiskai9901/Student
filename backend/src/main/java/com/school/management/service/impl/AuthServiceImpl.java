package com.school.management.service.impl;

import cn.hutool.core.util.StrUtil;
import com.school.management.dto.*;
import com.school.management.entity.User;
import com.school.management.enums.UserStatus;
import com.school.management.exception.BusinessException;
import com.school.management.entity.Department;
import com.school.management.mapper.ClassMapper;
import com.school.management.mapper.DepartmentMapper;
import com.school.management.mapper.UserMapper;
import com.school.management.security.CustomUserDetails;
import com.school.management.security.JwtTokenService;
import com.school.management.service.AuthService;
import com.school.management.util.PasswordValidator;
import com.school.management.util.WxMiniappUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final DepartmentMapper departmentMapper;
    private final ClassMapper classMapper;
    private final WxMiniappUtil wxMiniappUtil;
    private final PasswordEncoder passwordEncoder;

    private static final String LOGIN_ATTEMPT_KEY = "login:attempt:";
    private static final String USER_SESSION_KEY = "user:session:";
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;

    // 本地登录尝试缓存（Redis不可用时的后备方案）
    private static final ConcurrentHashMap<String, LoginAttemptInfo> localLoginAttempts = new ConcurrentHashMap<>();

    // 连续Redis失败计数器（用于检测Redis持续不可用）
    private static final java.util.concurrent.atomic.AtomicInteger redisFailureCount = new java.util.concurrent.atomic.AtomicInteger(0);
    private static final int MAX_REDIS_FAILURES_BEFORE_STRICT_MODE = 3;

    /**
     * 本地登录尝试信息
     */
    private static class LoginAttemptInfo {
        int attempts;
        long lockoutExpireTime;
        long createdAt; // 记录创建时间，用于持久化恢复判断

        LoginAttemptInfo(int attempts, long lockoutExpireTime) {
            this.attempts = attempts;
            this.lockoutExpireTime = lockoutExpireTime;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isLocked() {
            return attempts >= MAX_LOGIN_ATTEMPTS && System.currentTimeMillis() < lockoutExpireTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() >= lockoutExpireTime;
        }
    }

    @Override
    public LoginResponse login(LoginRequest request, String clientIp) {
        String username = request.getUsername();

        // 检查登录尝试次数
        checkLoginAttempts(username, clientIp);

        try {
            // 进行身份认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // 检查用户状态
            if (!userDetails.isEnabled()) {
                throw new BusinessException("用户已被禁用");
            }

            // 生成令牌
            String accessToken = jwtTokenService.generateToken(userDetails.getUserId(),
                                                             userDetails.getUsername(),
                                                             userDetails.getRoles());
            String refreshToken = jwtTokenService.generateRefreshToken(userDetails.getUserId());

            // 清除登录失败记录
            clearLoginAttempts(username, clientIp);

            // 记录登录信息
            recordLoginInfo(userDetails.getUserId(), clientIp, request);

            // 构建响应
            return buildLoginResponse(accessToken, refreshToken, userDetails);

        } catch (BusinessException e) {
            // 记录登录失败
            recordFailedLoginAttempt(username, clientIp);
            throw e;
        } catch (Exception e) {
            // 记录登录失败
            log.error("登录过程发生异常: username={}, error={}", username, e.getMessage(), e);
            recordFailedLoginAttempt(username, clientIp);
            throw new BusinessException("用户名或密码错误");
        }
    }

    @Override
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // 从刷新令牌中获取用户ID
        Long userId;
        try {
            userId = jwtTokenService.getUserIdFromToken(refreshToken);
        } catch (Exception e) {
            throw new BusinessException("刷新令牌无效");
        }

        if (!jwtTokenService.validateRefreshToken(refreshToken, userId)) {
            throw new BusinessException("刷新令牌无效或已过期");
        }
        User user = userMapper.selectById(userId);

        if (user == null || !UserStatus.isEnabled(user.getStatus())) {
            throw new BusinessException("用户不存在或已被禁用");
        }

        // 查询用户角色和权限
        List<String> roles = userMapper.findRoleCodesByUserId(userId);
        List<String> permissions = userMapper.findPermissionCodesByUserId(userId);

        // 生成新的访问令牌
        String newAccessToken = jwtTokenService.generateToken(userId, user.getUsername(), roles);

        CustomUserDetails userDetails = new CustomUserDetails(user, roles, permissions);

        return buildLoginResponse(newAccessToken, refreshToken, userDetails);
    }

    @Override
    public void logout(String accessToken, LogoutRequest request) {
        if (StrUtil.isNotBlank(accessToken)) {
            // 将访问令牌加入黑名单
            jwtTokenService.blacklistToken(accessToken);
        }

        // 如果有刷新令牌，也将其加入黑名单
        if (StrUtil.isNotBlank(request.getRefreshToken())) {
            jwtTokenService.blacklistToken(request.getRefreshToken());
        }

        // 如果要退出所有设备
        if (Boolean.TRUE.equals(request.getLogoutAll())) {
            CustomUserDetails userDetails = getCurrentUserDetails();
            if (userDetails != null) {
                // 删除用户的所有会话
                String sessionKey = USER_SESSION_KEY + userDetails.getUserId();
                redisTemplate.delete(sessionKey);

                // 删除用户的刷新令牌
                String refreshKey = "refresh_token:" + userDetails.getUserId();
                redisTemplate.delete(refreshKey);
            }
        }

        // 清除安全上下文
        SecurityContextHolder.clearContext();
    }

    @Override
    public LoginResponse.UserInfo getCurrentUserInfo() {
        CustomUserDetails userDetails = getCurrentUserDetails();
        if (userDetails == null) {
            throw new BusinessException("用户未登录");
        }

        User user = userMapper.selectById(userDetails.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        return buildUserInfo(userDetails);
    }

    /**
     * 检查登录尝试次数
     * 安全策略：优先使用Redis，Redis不可用时使用本地缓存并进入严格模式
     */
    private void checkLoginAttempts(String username, String clientIp) {
        String attemptKey = LOGIN_ATTEMPT_KEY + username + ":" + clientIp;

        try {
            Integer attempts = (Integer) redisTemplate.opsForValue().get(attemptKey);

            if (attempts != null && attempts >= MAX_LOGIN_ATTEMPTS) {
                throw new BusinessException("登录失败次数过多，账户已被锁定" + LOCKOUT_DURATION_MINUTES + "分钟");
            }

            // Redis可用，重置失败计数器
            redisFailureCount.set(0);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // 记录Redis失败次数
            int failures = redisFailureCount.incrementAndGet();

            // 如果Redis持续失败，进入严格安全模式
            if (failures >= MAX_REDIS_FAILURES_BEFORE_STRICT_MODE) {
                log.error("【安全警告】Redis持续不可用(连续{}次失败)，进入严格登录保护模式。" +
                         "建议立即检查Redis连接！", failures);

                // 严格模式：更激进的本地缓存检查 + 全局速率限制
                checkLoginAttemptsStrictMode(attemptKey, username);
            } else {
                log.warn("Redis连接失败({}/{}次)，使用本地缓存检查登录尝试: {}",
                        failures, MAX_REDIS_FAILURES_BEFORE_STRICT_MODE, e.getMessage());
                checkLoginAttemptsLocal(attemptKey);
            }
        }
    }

    /**
     * 严格模式登录检查 - Redis不可用时的安全降级策略
     * 更激进的保护措施，防止暴力破解
     */
    private void checkLoginAttemptsStrictMode(String attemptKey, String username) {
        // 清理过期记录
        cleanupExpiredLocalAttempts();

        LoginAttemptInfo info = localLoginAttempts.get(attemptKey);

        // 严格模式下，降低允许的尝试次数（5次 -> 3次）
        int strictMaxAttempts = MAX_LOGIN_ATTEMPTS - 2;

        if (info != null) {
            if (info.isLocked()) {
                throw new BusinessException("登录失败次数过多，账户已被锁定" + LOCKOUT_DURATION_MINUTES + "分钟");
            }
            if (info.attempts >= strictMaxAttempts) {
                log.warn("【安全警告】严格模式下账户 {} 达到登录限制", username);
                throw new BusinessException("系统安全检查中，请稍后再试");
            }
        }

        // 全局速率限制：检查短时间内的登录请求总数
        long recentAttemptCount = localLoginAttempts.values().stream()
                .filter(a -> !a.isExpired())
                .count();

        // 如果1分钟内超过50个不同账户尝试登录，可能是暴力破解
        if (recentAttemptCount > 50) {
            log.error("【安全警告】检测到大量登录尝试({}个)，可能存在暴力破解攻击", recentAttemptCount);
            throw new BusinessException("系统安全检查中，请稍后再试");
        }
    }

    /**
     * 使用本地缓存检查登录尝试次数
     */
    private void checkLoginAttemptsLocal(String attemptKey) {
        // 清理过期记录（每次检查时进行简单清理）
        cleanupExpiredLocalAttempts();

        LoginAttemptInfo info = localLoginAttempts.get(attemptKey);
        if (info != null && info.isLocked()) {
            throw new BusinessException("登录失败次数过多，账户已被锁定" + LOCKOUT_DURATION_MINUTES + "分钟");
        }
    }

    /**
     * 清理过期的本地登录尝试记录
     */
    private void cleanupExpiredLocalAttempts() {
        // 限制清理频率，避免每次都遍历
        if (localLoginAttempts.size() > 100) {
            localLoginAttempts.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }
    }

    /**
     * 记录登录失败尝试
     */
    private void recordFailedLoginAttempt(String username, String clientIp) {
        String attemptKey = LOGIN_ATTEMPT_KEY + username + ":" + clientIp;

        try {
            Integer attempts = (Integer) redisTemplate.opsForValue().get(attemptKey);
            attempts = attempts == null ? 1 : attempts + 1;

            redisTemplate.opsForValue().set(attemptKey, attempts, LOCKOUT_DURATION_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            // Redis不可用时使用本地缓存记录
            log.warn("Redis连接失败,使用本地缓存记录登录失败: {}", e.getMessage());
            recordFailedLoginAttemptLocal(attemptKey);
        }
    }

    /**
     * 使用本地缓存记录登录失败尝试
     */
    private void recordFailedLoginAttemptLocal(String attemptKey) {
        long lockoutExpireTime = System.currentTimeMillis() + LOCKOUT_DURATION_MINUTES * 60 * 1000L;

        localLoginAttempts.compute(attemptKey, (key, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new LoginAttemptInfo(1, lockoutExpireTime);
            }
            return new LoginAttemptInfo(existing.attempts + 1, lockoutExpireTime);
        });
    }

    /**
     * 清除登录失败记录
     */
    private void clearLoginAttempts(String username, String clientIp) {
        String attemptKey = LOGIN_ATTEMPT_KEY + username + ":" + clientIp;

        try {
            redisTemplate.delete(attemptKey);
        } catch (Exception e) {
            // Redis不可用时清除本地缓存
            log.warn("Redis连接失败,清除本地缓存登录尝试记录: {}", e.getMessage());
        }

        // 同时清除本地缓存（保持一致性）
        localLoginAttempts.remove(attemptKey);
    }

    /**
     * 记录登录信息
     */
    private void recordLoginInfo(Long userId, String clientIp, LoginRequest request) {
        // 更新用户最后登录时间和IP
        try {
            User updateUser = new User();
            updateUser.setId(userId);
            updateUser.setLastLoginTime(java.time.LocalDateTime.now());
            updateUser.setLastLoginIp(clientIp);
            userMapper.updateById(updateUser);
        } catch (Exception e) {
            // 更新登录信息失败不影响登录流程
            log.warn("更新用户登录信息失败: userId={}, error={}", userId, e.getMessage());
        }

        // 记录用户会话信息
        try {
            String sessionKey = USER_SESSION_KEY + userId;
            LoginRequest.DeviceInfo deviceInfo = request.getDeviceInfo();
            if (deviceInfo != null) {
                redisTemplate.opsForHash().put(sessionKey, "deviceId", deviceInfo.getDeviceId());
                redisTemplate.opsForHash().put(sessionKey, "platform", deviceInfo.getPlatform());
                redisTemplate.opsForHash().put(sessionKey, "version", deviceInfo.getVersion());
                redisTemplate.opsForHash().put(sessionKey, "userAgent", deviceInfo.getUserAgent());
            }
            redisTemplate.opsForHash().put(sessionKey, "loginTime", System.currentTimeMillis());
            redisTemplate.opsForHash().put(sessionKey, "clientIp", clientIp);
            redisTemplate.expire(sessionKey, 30, TimeUnit.DAYS);
        } catch (Exception e) {
            // Redis不可用时忽略会话记录
            log.warn("Redis连接失败,跳过会话信息记录: {}", e.getMessage());
        }
    }

    /**
     * 构建登录响应
     */
    private LoginResponse buildLoginResponse(String accessToken, String refreshToken, CustomUserDetails userDetails) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(7200L) // 2小时，从配置文件获取
                .userInfo(buildUserInfo(userDetails))
                .build();
    }

    /**
     * 构建用户信息
     */
    private LoginResponse.UserInfo buildUserInfo(CustomUserDetails userDetails) {
        User user = userMapper.selectById(userDetails.getUserId());

        return LoginResponse.UserInfo.builder()
                .userId(userDetails.getUserId())
                .username(userDetails.getUsername())
                .realName(userDetails.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .gender(user.getGender())
                .status(userDetails.getStatus())
                .roles(userDetails.getRoles())
                .permissions(userDetails.getPermissions())
                .orgUnit(buildOrgUnitInfo(user.getOrgUnitId()))
                .classInfo(buildClassInfo(user.getClassId()))
                .assignedClasses(buildAssignedClasses(userDetails.getUserId()))
                .build();
    }

    /**
     * 构建用户分配的班级列表（班主任/副班主任）
     */
    private List<LoginResponse.UserInfo.AssignedClass> buildAssignedClasses(Long userId) {
        if (userId == null) {
            return null;
        }

        List<ClassResponse> classes = classMapper.selectByTeacherId(userId);
        if (classes == null || classes.isEmpty()) {
            return null;
        }

        return classes.stream()
                .map(c -> {
                    String role;
                    if (userId.equals(c.getTeacherId())) {
                        role = "HEAD_TEACHER";
                    } else if (userId.equals(c.getAssistantTeacherId())) {
                        role = "DEPUTY_HEAD_TEACHER";
                    } else {
                        role = "SUBJECT_TEACHER";
                    }
                    return LoginResponse.UserInfo.AssignedClass.builder()
                            .id(c.getId())
                            .className(c.getClassName())
                            .role(role)
                            .build();
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 构建组织单元信息
     */
    private LoginResponse.UserInfo.OrgUnitInfo buildOrgUnitInfo(Long orgUnitId) {
        if (orgUnitId == null) {
            return null;
        }

        Department department = departmentMapper.selectById(orgUnitId);
        if (department == null) {
            return null;
        }

        return LoginResponse.UserInfo.OrgUnitInfo.builder()
                .orgUnitId(orgUnitId)
                .orgUnitName(department.getDeptName())
                .build();
    }

    /**
     * 构建班级信息
     */
    private LoginResponse.UserInfo.ClassInfo buildClassInfo(Long classId) {
        if (classId == null) {
            return null;
        }

        com.school.management.entity.Class classInfo = classMapper.selectById(classId);
        if (classInfo == null) {
            return null;
        }

        return LoginResponse.UserInfo.ClassInfo.builder()
                .classId(classId)
                .className(classInfo.getClassName())
                .build();
    }

    /**
     * 获取当前用户详情
     */
    private CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) authentication.getPrincipal();
        }
        return null;
    }

    @Override
    public WxLoginResponse wxLogin(WxLoginRequest request, String clientIp) {
        log.info("微信小程序登录请求, code长度: {}", request.getCode() != null ? request.getCode().length() : 0);

        // 调用微信API获取openid
        WxMiniappUtil.WxSession session = wxMiniappUtil.getSessionByCode(request.getCode());
        String openId = session.getOpenId();

        // 查询是否已绑定系统账号
        User user = userMapper.findByWechatOpenid(openId);

        if (user == null) {
            // 未绑定，返回openid供前端进行绑定操作
            log.info("微信用户未绑定系统账号, openId: {}", openId);
            return WxLoginResponse.notBound(openId);
        }

        // 已绑定，直接登录
        return wxLoginForUser(user, clientIp, request);
    }

    @Override
    public LoginResponse wxBind(WxBindRequest request, String clientIp) {
        log.info("微信账号绑定请求, username: {}", request.getUsername());

        // 先通过code获取openid
        WxMiniappUtil.WxSession session = wxMiniappUtil.getSessionByCode(request.getCode());
        String openId = session.getOpenId();

        // 检查openid是否已绑定其他账号
        if (userMapper.existsByWechatOpenid(openId)) {
            throw new BusinessException("该微信已绑定其他账号");
        }

        // 验证用户名密码
        User user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!UserStatus.isEnabled(user.getStatus())) {
            throw new BusinessException("账户已被禁用");
        }

        // 检查该用户是否已绑定其他微信
        if (StrUtil.isNotBlank(user.getWechatOpenid())) {
            throw new BusinessException("该账号已绑定其他微信");
        }

        // 绑定微信openid
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setWechatOpenid(openId);
        userMapper.updateById(updateUser);

        log.info("微信绑定成功, userId: {}, openId: {}", user.getId(), openId);

        // 绑定成功后直接登录
        WxLoginRequest loginRequest = new WxLoginRequest();
        loginRequest.setCode(request.getCode());
        loginRequest.setDeviceInfo(request.getDeviceInfo());

        return wxLoginForUserDirect(user, clientIp, request.getDeviceInfo());
    }

    @Override
    public boolean checkWxBinding(String openId) {
        return userMapper.existsByWechatOpenid(openId);
    }

    @Override
    public void updateProfile(ProfileUpdateRequest request) {
        CustomUserDetails userDetails = getCurrentUserDetails();
        if (userDetails == null) {
            throw new BusinessException("用户未登录");
        }

        User user = userMapper.selectById(userDetails.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 构建更新对象
        User updateUser = new User();
        updateUser.setId(user.getId());

        if (request.getRealName() != null) {
            updateUser.setRealName(request.getRealName());
        }
        if (request.getPhone() != null) {
            // 检查手机号是否被其他用户使用
            User existUser = userMapper.findByPhone(request.getPhone());
            if (existUser != null && !existUser.getId().equals(user.getId())) {
                throw new BusinessException("该手机号已被其他用户使用");
            }
            updateUser.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            // 检查邮箱是否被其他用户使用
            User existUser = userMapper.findByEmail(request.getEmail());
            if (existUser != null && !existUser.getId().equals(user.getId())) {
                throw new BusinessException("该邮箱已被其他用户使用");
            }
            updateUser.setEmail(request.getEmail());
        }
        if (request.getAvatar() != null) {
            updateUser.setAvatar(request.getAvatar());
        }
        if (request.getGender() != null) {
            updateUser.setGender(request.getGender());
        }

        userMapper.updateById(updateUser);
        log.info("用户更新个人资料成功: userId={}", user.getId());
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        CustomUserDetails userDetails = getCurrentUserDetails();
        if (userDetails == null) {
            throw new BusinessException("用户未登录");
        }

        // 检查新密码和确认密码是否一致
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("新密码与确认密码不一致");
        }

        User user = userMapper.selectById(userDetails.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码不正确");
        }

        // 检查新密码不能与旧密码相同
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与旧密码相同");
        }

        // 验证新密码强度
        PasswordValidator.validate(request.getNewPassword());

        // 更新密码
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        updateUser.setPasswordChangedAt(java.time.LocalDateTime.now());

        userMapper.updateById(updateUser);
        log.info("用户修改密码成功: userId={}", user.getId());
    }

    /**
     * 微信用户登录（已绑定）
     */
    private WxLoginResponse wxLoginForUser(User user, String clientIp, WxLoginRequest request) {
        if (!UserStatus.isEnabled(user.getStatus())) {
            throw new BusinessException("账户已被禁用");
        }

        LoginResponse loginResponse = wxLoginForUserDirect(user, clientIp, request.getDeviceInfo());
        return WxLoginResponse.bound(loginResponse);
    }

    /**
     * 为用户直接生成登录凭证
     */
    private LoginResponse wxLoginForUserDirect(User user, String clientIp, LoginRequest.DeviceInfo deviceInfo) {
        // 查询用户角色和权限
        List<String> roles = userMapper.findRoleCodesByUserId(user.getId());
        List<String> permissions = userMapper.findPermissionCodesByUserId(user.getId());

        // 生成令牌
        String accessToken = jwtTokenService.generateToken(user.getId(), user.getUsername(), roles);
        String refreshToken = jwtTokenService.generateRefreshToken(user.getId());

        // 记录登录信息
        try {
            User updateUser = new User();
            updateUser.setId(user.getId());
            updateUser.setLastLoginTime(java.time.LocalDateTime.now());
            updateUser.setLastLoginIp(clientIp);
            userMapper.updateById(updateUser);
        } catch (Exception e) {
            log.warn("更新用户登录信息失败: userId={}, error={}", user.getId(), e.getMessage());
        }

        // 记录会话信息
        try {
            String sessionKey = USER_SESSION_KEY + user.getId();
            if (deviceInfo != null) {
                redisTemplate.opsForHash().put(sessionKey, "deviceId", deviceInfo.getDeviceId());
                redisTemplate.opsForHash().put(sessionKey, "platform", deviceInfo.getPlatform());
                redisTemplate.opsForHash().put(sessionKey, "version", deviceInfo.getVersion());
            }
            redisTemplate.opsForHash().put(sessionKey, "loginTime", System.currentTimeMillis());
            redisTemplate.opsForHash().put(sessionKey, "clientIp", clientIp);
            redisTemplate.opsForHash().put(sessionKey, "loginType", "miniapp");
            redisTemplate.expire(sessionKey, 30, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("Redis连接失败,跳过会话信息记录: {}", e.getMessage());
        }

        // 构建用户详情
        CustomUserDetails userDetails = new CustomUserDetails(user, roles, permissions);

        return buildLoginResponse(accessToken, refreshToken, userDetails);
    }
}