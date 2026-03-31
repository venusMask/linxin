package org.venus.lin.xin.mgr.business.model.request;

import lombok.Data;

import java.util.List;

@Data
public class AddGroupMembersRequest {
    private List<Long> memberIds;
}
