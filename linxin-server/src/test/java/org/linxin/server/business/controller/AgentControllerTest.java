package org.linxin.server.business.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.linxin.server.auth.JwtService;
import org.linxin.server.auth.DPUserDetailLoginService;
import org.linxin.server.business.mapper.UserMapper;
import org.linxin.server.business.service.IAgentService;
import org.linxin.server.business.service.IAgentTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgentController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IAgentService agentService;

    @MockBean
    private IAgentTokenService agentTokenService;

    // 必须 Mock 这些 Bean 以满足 JwtAuthenticationFilter 的构造需求，从而让 SecurityConfig 加载成功
    @MockBean private JwtService jwtService;
    @MockBean private DPUserDetailLoginService userDetailsService;
    @MockBean private UserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    public void testGetManifest() throws Exception {
        Map<String, Object> mockManifest = Map.of("version", "1.0.0");
        when(agentService.getManifest()).thenReturn(mockManifest);

        mockMvc.perform(get("/api/agent/manifest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.version").value("1.0.0"));
    }

    @Test
    @WithMockUser
    public void testCallTool() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("tool", "send_message");
        request.put("arguments", Map.of("recipient", "媳妇", "content", "hi"));
        request.put("agentName", "OpenClaw");

        Map<String, Object> mockResult = Map.of("status", "SUCCESS");
        when(agentService.callTool(anyLong(), anyString(), anyMap(), anyString())).thenReturn(mockResult);

        mockMvc.perform(post("/api/agent/call")
                        .with(csrf())
                        .requestAttr("userId", 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }
}
