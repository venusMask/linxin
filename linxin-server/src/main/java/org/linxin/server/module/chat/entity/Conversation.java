package org.linxin.server.module.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("conversations")
public class Conversation {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long peerId;

    private String peerNickname;

    private String peerAvatar;

    private Integer type;

    private Long groupId;

    private Long lastMessageId;

    private String lastMessageContent;

    private Integer lastMessageType;

    private LocalDateTime lastMessageTime;

    private Integer unreadCount;

    private Integer topStatus;

    private Integer muteStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
