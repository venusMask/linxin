package org.linxin.server.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

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
