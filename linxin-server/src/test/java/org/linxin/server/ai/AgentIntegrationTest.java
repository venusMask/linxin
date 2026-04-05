package org.linxin.server.ai;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.linxin.server.ai.core.agent.AIAgent;
import org.linxin.server.ai.core.dto.ModelResponse;
import org.linxin.server.module.chat.service.IMessageService;
import org.linxin.server.module.contact.service.IFriendService;
import org.linxin.server.module.group.service.IGroupService;
import org.linxin.server.module.group.vo.GroupVO;
import org.linxin.server.module.user.entity.User;
import org.linxin.server.module.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
public class AgentIntegrationTest {

    @Autowired
    private AIAgent aiAgent;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private IFriendService friendService;

    @Autowired
    private IGroupService groupService;

    @Autowired
    private IMessageService messageService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private User userA;
    private User userB;

    @BeforeEach
    public void setup() {
        // 彻底物理重置业务数据，保留 AI 助手 (ID=1)
        try {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
            jdbcTemplate.execute("DELETE FROM users WHERE username IN ('test_user_a', 'test_user_b')");
            jdbcTemplate.execute("TRUNCATE TABLE friends");
            jdbcTemplate.execute("TRUNCATE TABLE friend_apply");
            jdbcTemplate.execute("TRUNCATE TABLE conversations");
            jdbcTemplate.execute("TRUNCATE TABLE messages");
            jdbcTemplate.execute("TRUNCATE TABLE groups");
            jdbcTemplate.execute("TRUNCATE TABLE group_members");
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");

            // 检查 ID=1 的 AI 助手是否存在，不存在则创建
            try {
                if (userMapper.selectById(1L) == null) {
                    createSystemAIUser();
                }
            } catch (Exception e) {
                createSystemAIUser();
            }

        } catch (Exception e) {
            System.err.println("Setup cleanup failed: " + e.getMessage());
        }

        userA = createTestUser("test_user_a", "测试用户A");
        userB = createTestUser("test_user_b", "测试用户B");

        System.out.println("Created Test Users: A=" + userA.getId() + ", B=" + userB.getId());
    }

    private void createSystemAIUser() {
        User aiUser = new User();
        aiUser.setId(1L);
        aiUser.setUsername("ai_assistant");
        aiUser.setNickname("AI 助手");
        aiUser.setPassword(passwordEncoder.encode("system_protected"));
        aiUser.setUserType(1);
        aiUser.setStatus(1);
        userMapper.insert(aiUser);
    }
    private User createTestUser(String username, String nickname) {
        User user = new User();
        user.setUsername(username);
        user.setNickname(nickname);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setUserType(0);
        user.setStatus(1);
        userMapper.insert(user);
        return user;
    }

    @Test
    public void testSingleStep_SendMessage() {
        // 前置条件：建立好友关系
        forceBeFriends(userA.getId(), userB.getId());

        String input = String.format("给 %s 发送消息：这是一条来自AI的单步测试消息", userB.getUsername());

        System.out.println("--- Single Step Test Starting ---");
        ModelResponse response = aiAgent.run(userA.getId(), input, java.util.Collections.emptyList());

        System.out.println("AI Response: " + response.getContent());
        assertNotNull(response.getContent());
    }

    @Test
    public void testMultiStep_ComplexScenario() {
        String randomGroupName = "测试群" + (System.currentTimeMillis() % 1000);
        String input = String.format(
                "请执行以下操作：\n" +
                        "1. 添加用户 %s 为好友。\n" +
                        "2. 创建一个名为 '%s' 的群聊并把我们都拉进去。\n" +
                        "3. 在群里发一句 'Agent集成测试成功'。",
                userB.getUsername(), randomGroupName);

        System.out.println("--- Multi-step Agent Test Starting ---");
        ModelResponse response = aiAgent.run(userA.getId(), input, java.util.Collections.emptyList());

        System.out.println("--- Agent Final Answer ---");
        System.out.println(response.getContent());

        // 验证结果
        List<GroupVO> groups = groupService.getUserGroups(userA.getId());
        boolean groupCreated = groups.stream().anyMatch(g -> g.getName().equals(randomGroupName));
        System.out.println("Verification - Group Created: " + groupCreated);

        assertNotNull(response.getContent());
    }

    private void forceBeFriends(Long u1, Long u2) {
        // 模拟直接成为好友的逻辑（绕过申请确认）
        try {
            java.lang.reflect.Method method = org.linxin.server.module.contact.service.impl.FriendServiceImpl.class
                    .getDeclaredMethod("addFriendPair", Long.class, Long.class);
            method.setAccessible(true);
            method.invoke(friendService, u1, u2);
        } catch (Exception e) {
            System.err.println("Failed to force be friends in test: " + e.getMessage());
        }
    }
}
