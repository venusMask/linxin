package org.linxin.server.module.contact.vo;

import java.time.LocalDateTime;
import lombok.Data;

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
