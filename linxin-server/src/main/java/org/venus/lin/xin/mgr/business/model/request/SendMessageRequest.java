package org.venus.lin.xin.mgr.business.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequest {
    private Long receiverId;

    private Integer messageType;

    private String content;

    private String extra;

    private Integer conversationType;

    private Long groupId;
}
