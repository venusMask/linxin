package org.linxin.server.business.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private Integer gender;
    private String signature;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
}
