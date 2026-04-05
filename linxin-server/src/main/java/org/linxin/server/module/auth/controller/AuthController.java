package org.linxin.server.module.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.linxin.server.auth.JwtService;
import org.linxin.server.common.result.Result;
import org.linxin.server.module.auth.service.IEmailVerificationService;
import org.linxin.server.module.user.converter.UserConverter;
import org.linxin.server.module.user.entity.User;
import org.linxin.server.module.user.model.request.SendEmailCodeRequest;
import org.linxin.server.module.user.model.request.UpdateEmailRequest;
import org.linxin.server.module.user.model.request.UpdatePasswordRequest;
import org.linxin.server.module.user.model.request.UpdateProfileRequest;
import org.linxin.server.module.user.model.request.UserLoginRequest;
import org.linxin.server.module.user.model.request.UserRegisterRequest;
import org.linxin.server.module.user.service.IUserService;
import org.linxin.server.module.user.vo.UserVO;
import org.springframework.web.bind.annotation.*;

/**
 * insert into `users`(username, nickname, password, status, user_type) values ('test_admin', '测试',
 * '$2a$10$8.UnVuG9HHgffUDAlk8q2OuVGkqEnLPzDbaW2CLvIDJAlBDW6E7ve', 1, 0)
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证模块", description = "用户注册、登录相关接口")
public class AuthController {

    private final IUserService userService;
    private final IEmailVerificationService emailVerificationService;
    private final JwtService jwtService;
    private final UserConverter userConverter;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<UserVO> register(@Valid @RequestBody UserRegisterRequest request) {
        User user = userService.register(request);
        return Result.success(userConverter.toVO(user));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<Map<String, Object>> login(@Valid @RequestBody UserLoginRequest request) {
        User user = userService.login(request);
        String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getPasswordVersion());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        return Result.success(data);
    }

    @GetMapping("/userinfo")
    @Operation(summary = "获取当前用户信息")
    public Result<UserVO> getUserInfo(@RequestAttribute("userId") Long userId) {
        UserVO userVO = userService.getUserInfo(userId);
        return Result.success(userVO);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索用户")
    public Result<List<UserVO>> searchUsers(@RequestParam String keyword) {
        List<UserVO> users = userService.searchUsers(keyword);
        return Result.success(users);
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "获取指定用户信息")
    public Result<UserVO> getUserById(@PathVariable Long userId) {
        UserVO userVO = userService.getUserById(userId);
        if (userVO == null) {
            return Result.error("用户不存在");
        }
        return Result.success(userVO);
    }

    @PostMapping("/email/send-code")
    @Operation(summary = "发送邮箱验证码")
    public Result<Void> sendEmailCode(@Valid @RequestBody SendEmailCodeRequest request) {
        String type = request.getType() != null ? request.getType() : "register";
        emailVerificationService.sendVerificationCode(request.getEmail(), type);
        return Result.success(null);
    }

    @PutMapping("/profile")
    @Operation(summary = "更新个人资料")
    public Result<Void> updateProfile(@RequestAttribute("userId") Long userId,
            @RequestBody UpdateProfileRequest request) {
        userService.updateProfile(userId, request);
        return Result.success(null);
    }

    @PutMapping("/email")
    @Operation(summary = "更换绑定邮箱")
    public Result<Void> updateEmail(@RequestAttribute("userId") Long userId,
            @Valid @RequestBody UpdateEmailRequest request) {
        userService.updateEmail(userId, request);
        return Result.success(null);
    }

    @PutMapping("/password")
    @Operation(summary = "修改登录密码")
    public Result<Void> updatePassword(@RequestAttribute("userId") Long userId,
            @Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(userId, request);
        return Result.success(null);
    }
}
