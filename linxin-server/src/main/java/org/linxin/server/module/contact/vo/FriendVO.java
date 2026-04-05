package org.linxin.server.module.contact.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FriendVO {
    private Long id;
    private Long userId;
    private Long friendId;
    private String username;
    private String nickname;
    private String avatar;
    private String friendNickname;
    private String friendGroup;
    private String tags;
    private String signature;
    private Integer userStatus;
    private Integer userType;
    private Integer deleted;
    private Long sequenceId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
