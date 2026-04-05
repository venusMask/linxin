package org.linxin.server.module.contact.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FriendUpdateRequest {
    @NotNull(message = "好友ID不能为空")
    private Long friendId;

    private String friendNickname;
    private String friendGroup;

    private String tags;
}
