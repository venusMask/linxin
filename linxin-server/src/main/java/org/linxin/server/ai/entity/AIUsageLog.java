package org.linxin.server.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

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
