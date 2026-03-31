package org.linxin.server.business.model.request;

import lombok.Data;

import java.util.List;

@Data
public class CreateGroupRequest {
    private String name;
    private String avatar;
    private List<Long> memberIds;
}
