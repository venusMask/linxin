package org.linxin.server.module.contact.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FriendApplyRequest {
    @NotNull(message = "被申请用户ID不能为空")
    private Long friendId;

    private String remark;
}
