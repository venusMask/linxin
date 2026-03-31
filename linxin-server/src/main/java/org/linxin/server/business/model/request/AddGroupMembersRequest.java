package org.linxin.server.business.model.request;

import lombok.Data;

import java.util.List;

@Data
public class AddGroupMembersRequest {
    private List<Long> memberIds;
}
