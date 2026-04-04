package org.linxin.server.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("group_members")
public class GroupMember {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;

    private Long userId;

    private String nickname;

    private Integer role;

    private LocalDateTime joinTime;

    private Integer muteStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
