package org.linxin.server.module.chat.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MessageVO {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderNickname;
    private String senderAvatar;
    private Long receiverId;

    private Long groupId;

    private Integer conversationType;

    private Integer messageType;
    private String content;
    private String extra;
    private Integer sendStatus;
    private LocalDateTime sendTime;

    private Long sequenceId;
    private Boolean isAi;
    private String senderType;
    private Integer userType; // 发送者用户类型
}
