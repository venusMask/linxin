package org.venus.lin.xin.mgr.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

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
