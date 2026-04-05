package org.linxin.server.module.contact.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("friend_apply")
public class FriendApply {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fromUserId;

    private Long toUserId;

    private String fromNickname;

    private String fromAvatar;

    private String remark;

    private Integer status;

    private Integer readStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private LocalDateTime handleTime;

    @TableLogic
    private Integer deleted;
}
