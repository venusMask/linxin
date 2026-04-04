package org.linxin.server.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_usage_logs")
public class AIUsageLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String providerName;
    
    private String modelName;
    
    private String intent;
    
    private Integer promptTokens;
    
    private Integer completionTokens;
    
    private Integer totalTokens;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
