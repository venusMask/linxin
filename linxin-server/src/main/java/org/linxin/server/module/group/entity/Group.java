package org.linxin.server.module.group.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("`groups`")
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
