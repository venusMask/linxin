package org.venus.lin.xin.mgr.business.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FriendApplyRequest {
    @NotNull(message = "被申请用户ID不能为空")
    private Long toUserId;
    
    private String remark;
}
