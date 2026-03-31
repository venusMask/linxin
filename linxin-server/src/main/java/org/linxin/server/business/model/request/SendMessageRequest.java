package org.linxin.server.business.model.request;

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
