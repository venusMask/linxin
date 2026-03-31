package org.linxin.server.business.model.request;

import lombok.Data;

@Data
public class FriendListRequest {
    private String username;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
