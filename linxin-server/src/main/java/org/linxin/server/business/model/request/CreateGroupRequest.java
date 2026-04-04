package org.linxin.server.business.model.request;

import java.util.List;
import lombok.Data;

@Data
public class CreateGroupRequest {
    private String name;
    private String avatar;
    private List<Long> memberIds;
}
