package org.linxin.server.business.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FriendHandleRequest {
    @NotNull(message = "申请ID不能为空")
    private Long applyId;
    
    @NotNull(message = "处理状态不能为空")
    private Integer status;
}
