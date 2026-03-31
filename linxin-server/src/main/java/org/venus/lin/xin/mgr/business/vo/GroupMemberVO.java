package org.venus.lin.xin.mgr.business.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupMemberVO {
    private Long id;
    private Long groupId;
    private Long userId;
    private String nickname;
    private String avatar;
    private Integer role;
    private LocalDateTime joinTime;
    private Integer muteStatus;
}
