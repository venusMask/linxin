package org.linxin.server.module.contact.model.request;

import lombok.Data;

@Data
public class FriendListRequest {
    private String username;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
