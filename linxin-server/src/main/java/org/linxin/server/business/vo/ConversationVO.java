package org.linxin.server.business.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ConversationVO {
    private Long id;
    private String conversationKey;
    private Long peerId;
    private String peerNickname;
    private String peerAvatar;

    private Integer type;

    private Long groupId;

    private String groupName;

    private Long lastMessageId;
    private String lastMessageContent;
    private Integer lastMessageType;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
    private Integer topStatus;
    private Integer muteStatus;
}
