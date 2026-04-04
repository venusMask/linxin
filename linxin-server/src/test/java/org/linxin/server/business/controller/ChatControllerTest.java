package org.linxin.server.business.controller;

import org.junit.jupiter.api.Test;
import org.linxin.server.auth.JwtService;
import org.linxin.server.auth.DPUserDetailLoginService;
import org.linxin.server.business.mapper.UserMapper;
import org.linxin.server.business.service.IChatService;
import org.linxin.server.business.service.IAgentTokenService;
import org.linxin.server.business.vo.MessageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private IChatService chatService;
    
    // Security 依赖
    @MockBean private JwtService jwtService;
    @MockBean private IAgentTokenService agentTokenService;
    @MockBean private DPUserDetailLoginService userDetailsService;
    @MockBean private UserMapper userMapper;

    @Test
    public void testSyncMessages() throws Exception {
        MessageVO mockMsg = new MessageVO();
        mockMsg.setContent("Sync Test");
        mockMsg.setSequenceId(1005L);

        when(chatService.syncMessages(anyLong(), anyLong())).thenReturn(List.of(mockMsg));

        mockMvc.perform(get("/chat/sync")
                        .requestAttr("userId", 1001L)
                        .param("lastSequenceId", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("Sync Test"))
                .andExpect(jsonPath("$.data[0].sequenceId").value(1005));
    }
}
