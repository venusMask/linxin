package org.linxin.server.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("groups")
public class Group {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String avatar;

    private Long ownerId;

    private String announcement;

    private Integer memberLimit;

    private Integer memberCount;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
