package org.linxin.server.business.vo;

import java.time.LocalDateTime;
import lombok.Data;

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
    private Integer userType;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
}
