package org.linxin.server.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linxin.server.business.converter.UserConverter;
import org.linxin.server.business.entity.User;
import org.linxin.server.business.mapper.UserMapper;
import org.linxin.server.business.model.request.UserLoginRequest;
import org.linxin.server.business.model.request.UserRegisterRequest;
import org.linxin.server.business.service.IEmailVerificationService;
import org.linxin.server.business.service.impl.UserServiceImpl;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserConverter userConverter;
    @Mock
    private IEmailVerificationService emailVerificationService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    public void testRegister_Success() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("newuser");
        request.setPassword("pass");
        request.setNickname("Nick");
        request.setEmail("test@example.com");
        request.setVerificationCode("123456");

        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(emailVerificationService.verifyCode("test@example.com", "123456")).thenReturn(true);

        userService.register(request);

        verify(userMapper, times(1)).insert(any(User.class));
        verify(emailVerificationService, times(1)).verifyCode("test@example.com", "123456");
    }

    @Test
    public void testLogin_Success() {
        UserLoginRequest request = new UserLoginRequest();
        request.setUsername("testuser");
        request.setPassword("rightpass");

        User mockUser = new User();
        mockUser.setUsername("testuser");
        mockUser.setPassword("encoded");
        mockUser.setStatus(1);

        when(userMapper.selectOne(any())).thenReturn(mockUser);
        when(passwordEncoder.matches("rightpass", "encoded")).thenReturn(true);

        User result = userService.login(request);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    public void testLogin_WrongPassword() {
        UserLoginRequest request = new UserLoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpass");

        User mockUser = new User();
        mockUser.setPassword("encoded");
        mockUser.setStatus(1);

        when(userMapper.selectOne(any())).thenReturn(mockUser);
        when(passwordEncoder.matches("wrongpass", "encoded")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> userService.login(request));
    }
}
