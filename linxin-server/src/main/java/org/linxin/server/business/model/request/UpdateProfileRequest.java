package org.linxin.server.business.model.request;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String nickname;
    private String username;
    private String avatar;
    private String signature;
    private Integer gender;
}
