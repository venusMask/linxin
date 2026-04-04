package org.linxin.server.business.vo;

import lombok.Data;

import java.time.LocalDateTime;

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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
