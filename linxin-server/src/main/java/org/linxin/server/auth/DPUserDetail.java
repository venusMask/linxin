package org.linxin.server.auth;

import lombok.Data;

@Data
public class DPUserDetail {

    private Long userId;
    private String username;
    private String password;

}
