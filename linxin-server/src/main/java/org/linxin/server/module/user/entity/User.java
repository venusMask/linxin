package org.linxin.server.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String phone;

    private String email;

    private String password;

    private Integer gender;

    private String signature;

    private Integer status;

    private Integer passwordVersion;

    /**
     * 用户类型: 0-普通用户, 1-系统AI
     */
    private Integer userType;

    private LocalDateTime lastLoginTime;

    private String lastLoginIp;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
