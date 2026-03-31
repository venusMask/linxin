package org.venus.lin.xin.mgr.business.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.venus.lin.xin.mgr.auth.JwtService;
import org.venus.lin.xin.mgr.business.entity.User;
import org.venus.lin.xin.mgr.business.model.request.UserLoginRequest;
import org.venus.lin.xin.mgr.business.model.request.UserRegisterRequest;
import org.venus.lin.xin.mgr.business.service.IUserService;
import org.venus.lin.xin.mgr.business.vo.UserVO;
import org.venus.lin.xin.mgr.common.result.Result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证模块", description = "用户注册、登录相关接口")
public class AuthController {

    private final IUserService userService;
    private final JwtService jwtService;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<User> register(@Valid @RequestBody UserRegisterRequest request) {
        User user = userService.register(request);
        return Result.success(user);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<Map<String, Object>> login(@Valid @RequestBody UserLoginRequest request) {
        User user = userService.login(request);
        String token = jwtService.generateToken(user.getId(), user.getUsername());
        
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        return Result.success(data);
    }

    @GetMapping("/userinfo")
    @Operation(summary = "获取当前用户信息")
    public Result<UserVO> getUserInfo(@RequestHeader("X-User-Id") Long userId) {
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
}
