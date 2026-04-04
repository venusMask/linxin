package org.linxin.server.business.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class GroupVO {
    private Long id;
    private String name;
    private String avatar;
    private Long ownerId;
    private String ownerNickname;
    private String announcement;
    private Integer memberLimit;
    private Integer memberCount;
    private Integer status;
    private LocalDateTime createTime;
    private List<GroupMemberVO> members;
}
