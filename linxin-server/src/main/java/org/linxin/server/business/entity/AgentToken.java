package org.linxin.server.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("agent_tokens")
public class AgentToken {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String token;

    private String agentName;

    private String scopes;

    private Integer status;

    private LocalDateTime lastUsedTime;

    private LocalDateTime expireTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
