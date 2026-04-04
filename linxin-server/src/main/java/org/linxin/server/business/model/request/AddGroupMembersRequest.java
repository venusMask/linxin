package org.linxin.server.business.model.request;

import java.util.List;
import lombok.Data;

@Data
public class AddGroupMembersRequest {
    private List<Long> memberIds;
}
