package org.linxin.server.module.group.model.request;

import java.util.List;
import lombok.Data;

@Data
public class AddGroupMembersRequest {
    private List<Long> memberIds;
}
