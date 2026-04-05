package org.linxin.server.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linxin.server.common.exception.BusinessException;
import org.linxin.server.module.auth.service.IEmailVerificationService;
import org.linxin.server.module.user.converter.UserConverter;
import org.linxin.server.module.user.entity.User;
import org.linxin.server.module.user.mapper.UserMapper;
import org.linxin.server.module.user.model.request.UpdateEmailRequest;
import org.linxin.server.module.user.model.request.UpdatePasswordRequest;
import org.linxin.server.module.user.model.request.UpdateProfileRequest;
import org.linxin.server.module.user.model.request.UserLoginRequest;
import org.linxin.server.module.user.model.request.UserRegisterRequest;
import org.linxin.server.module.user.service.IUserService;
import org.linxin.server.module.user.vo.UserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserConverter userConverter;
    private final IEmailVerificationService emailVerificationService;

    @PostConstruct
    public void init() {
        initSystemAI();
    }

    private void initSystemAI() {
        try {
            User aiUser = userMapper.selectById(1L);
            if (aiUser == null) {
                log.info("初始化系统 AI 助手...");
                aiUser = new User();
                aiUser.setId(1L);
                aiUser.setUsername("ai_assistant");
                aiUser.setNickname("AI 助手");
                aiUser.setPassword(passwordEncoder.encode("system_protected_" + System.currentTimeMillis()));
                aiUser.setUserType(1);
                aiUser.setStatus(1);
                aiUser.setGender(0);
                userMapper.insert(aiUser);
            } else if (aiUser.getUserType() == null || aiUser.getUserType() != 1) {
                log.info("更新系统 AI 助手类型...");
                aiUser.setUserType(1);
                userMapper.updateById(aiUser);
            }
        } catch (Exception e) {
            log.error("Failed to init system AI user", e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public User register(UserRegisterRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        LambdaQueryWrapper<User> emailWrapper = new LambdaQueryWrapper<>();
        emailWrapper.eq(User::getEmail, request.getEmail());
        if (userMapper.selectCount(emailWrapper) > 0) {
            throw new RuntimeException("该邮箱已被注册");
        }

        emailVerificationService.verifyCode(request.getEmail(), request.getVerificationCode());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setGender(0);
        user.setStatus(1);
        userMapper.insert(user);
        return user;
    }

    @Override
    public User login(UserLoginRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }

        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);
        return user;
    }

    @Override
    public UserVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return userConverter.toVO(user);
    }

    @Override
    public UserVO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return userConverter.toVO(user);
    }

    @Override
    public UserVO getUserByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            return null;
        }
        return userConverter.toVO(user);
    }

    @Override
    public List<UserVO> searchUsers(String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.like(User::getUsername, keyword)
                .or().like(User::getNickname, keyword)
                .or().like(User::getPhone, keyword));
        wrapper.eq(User::getStatus, 1);
        return userMapper.selectList(wrapper).stream()
                .map(userConverter::toVO)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, request.getUsername());
            if (userMapper.selectCount(wrapper) > 0) {
                throw new RuntimeException("用户名已存在");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getSignature() != null) {
            user.setSignature(request.getSignature());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        userMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateEmail(Long userId, UpdateEmailRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 1. 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("当前密码错误");
        }

        // 2. 验证新邮箱是否已被占用
        if (request.getNewEmail().equals(user.getEmail())) {
            throw new RuntimeException("新邮箱不能与原邮箱相同");
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, request.getNewEmail());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("该邮箱已被其他账号绑定");
        }

        // 3. 验证验证码
        emailVerificationService.verifyCode(request.getNewEmail(), request.getCode(), "change_email");

        // 4. 更新邮箱
        user.setEmail(request.getNewEmail());
        userMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        if (request.getNewPassword().length() < 6) {
            throw new BusinessException("新密码长度不能少于6位");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordVersion(user.getPasswordVersion() + 1);
        userMapper.updateById(user);
    }
}
