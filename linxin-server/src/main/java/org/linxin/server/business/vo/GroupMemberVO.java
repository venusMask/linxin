package org.linxin.server.business.vo;

import java.time.LocalDateTime;
import lombok.Data;

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
