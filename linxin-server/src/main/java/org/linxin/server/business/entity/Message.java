package org.linxin.server.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("messages")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long conversationId;

    private Long senderId;

    private Long receiverId;

    private Long groupId;

    private Integer messageType;

    private String content;

    private String extra;

    private Integer sendStatus;

    private LocalDateTime sendTime;

    private Long sequenceId;

    private String senderType;

    private Boolean isAi;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
