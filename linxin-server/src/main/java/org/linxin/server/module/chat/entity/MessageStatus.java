package org.linxin.server.module.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("message_status")
public class MessageStatus {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long messageId;

    private Long userId;

    private Integer readStatus;

    private LocalDateTime readTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
