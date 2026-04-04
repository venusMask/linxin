package org.linxin.server.business.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.linxin.server.business.entity.Conversation;
import org.linxin.server.business.entity.Message;
import org.linxin.server.business.model.request.SendMessageRequest;
import org.linxin.server.business.service.IChatService;
import org.linxin.server.business.vo.ConversationVO;
import org.linxin.server.business.vo.MessageVO;
import org.linxin.server.common.result.Result;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Tag(name = "聊天模块", description = "聊天相关操作接口")
public class ChatController {

    private final IChatService chatService;

    @GetMapping("/conversations")
    @Operation(summary = "获取会话列表")
    public Result<IPage<ConversationVO>> getConversationList(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        IPage<ConversationVO> conversations = chatService.getConversationList(userId, pageNum, pageSize);
        return Result.success(conversations);
    }

    @GetMapping("/conversations/{peerId}")
    @Operation(summary = "获取或创建与指定用户的会话")
    public Result<Conversation> getOrCreateConversation(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long peerId) {
        Conversation conversation = chatService.getOrCreateConversation(userId, peerId);
        return Result.success(conversation);
    }

    @PostMapping("/messages")
    @Operation(summary = "发送消息")
    public Result<Message> sendMessage(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody SendMessageRequest request) {
        Message message;
        if (request.getConversationType() != null && request.getConversationType() == 1) {
            message = chatService.sendGroupMessage(userId, request);
        } else {
            message = chatService.sendMessage(userId, request);
        }
        return Result.success(message);
    }

    @GetMapping("/group/messages")
    @Operation(summary = "获取群消息列表")
    public Result<IPage<MessageVO>> getGroupMessageList(
            @RequestAttribute("userId") Long userId,
            @RequestParam Long groupId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        IPage<MessageVO> messages = chatService.getGroupMessageList(groupId, userId, pageNum, pageSize);
        return Result.success(messages);
    }

    @GetMapping("/messages/{conversationId}")
    @Operation(summary = "获取消息列表")
    public Result<IPage<MessageVO>> getMessageList(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        IPage<MessageVO> messages = chatService.getMessageList(conversationId, pageNum, pageSize);
        return Result.success(messages);
    }

    @PostMapping("/messages/{conversationId}/read")
    @Operation(summary = "标记消息为已读")
    public Result<String> markAsRead(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long conversationId) {
        chatService.markMessagesAsRead(userId, conversationId);
        return Result.success("已标记为已读");
    }

    @PostMapping("/conversations/{conversationId}/top")
    @Operation(summary = "切换置顶状态")
    public Result<String> toggleTop(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long conversationId) {
        chatService.toggleTop(userId, conversationId);
        return Result.success("操作成功");
    }

    @PostMapping("/conversations/{conversationId}/mute")
    @Operation(summary = "切换静音状态")
    public Result<String> toggleMute(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long conversationId) {
        chatService.toggleMute(userId, conversationId);
        return Result.success("操作成功");
    }

    @GetMapping("/sync")
    @Operation(summary = "增量同步消息接口")
    public Result<List<MessageVO>> syncMessages(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false, defaultValue = "0") Long lastSequenceId) {
        List<MessageVO> messages = chatService.syncMessages(userId, lastSequenceId);
        return Result.success(messages);
    }
}
