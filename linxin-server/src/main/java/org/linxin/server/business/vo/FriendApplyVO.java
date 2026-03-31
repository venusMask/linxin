package org.linxin.server.business.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FriendApplyVO {
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private String fromNickname;
    private String fromAvatar;
    private String remark;
    private Integer status;
    private Integer readStatus;
    private LocalDateTime createTime;
    private LocalDateTime handleTime;
}
