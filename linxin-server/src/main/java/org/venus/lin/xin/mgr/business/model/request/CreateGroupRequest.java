package org.venus.lin.xin.mgr.business.model.request;

import lombok.Data;

import java.util.List;

@Data
public class CreateGroupRequest {
    private String name;
    private String avatar;
    private List<Long> memberIds;
}
