package org.linxin.server.business.vo;

import lombok.Data;

import java.time.LocalDateTime;

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
}
