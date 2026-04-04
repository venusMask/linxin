package org.linxin.server.business.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.linxin.server.auth.JwtService;
import org.linxin.server.auth.DPUserDetailLoginService;
import org.linxin.server.business.converter.UserConverter;
import org.linxin.server.business.entity.User;
import org.linxin.server.business.mapper.UserMapper;
import org.linxin.server.business.model.request.UserLoginRequest;
import org.linxin.server.business.model.request.UserRegisterRequest;
import org.linxin.server.business.service.IUserService;
import org.linxin.server.business.service.IAgentTokenService;
import org.linxin.server.business.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private IUserService userService;
    @MockBean private JwtService jwtService;
    @MockBean private UserConverter userConverter;
    
    // Security 依赖
    @MockBean private IAgentTokenService agentTokenService;
    @MockBean private DPUserDetailLoginService userDetailsService;
    @MockBean private UserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testRegister() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("test");
        request.setPassword("password123");
        request.setNickname("nick");

        when(userService.register(any())).thenReturn(new User());
        when(userConverter.toVO(any())).thenReturn(new UserVO());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    public void testLogin() throws Exception {
        UserLoginRequest request = new UserLoginRequest();
        request.setUsername("user");
        request.setPassword("pass");

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("user");

        when(userService.login(any())).thenReturn(mockUser);
        when(jwtService.generateToken(anyLong(), anyString())).thenReturn("jwt_token");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("jwt_token"));
    }
}
