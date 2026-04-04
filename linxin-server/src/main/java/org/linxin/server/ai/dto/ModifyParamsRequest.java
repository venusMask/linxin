package org.linxin.server.ai.dto;

import lombok.Data;

@Data
public class ModifyParamsRequest {
    private String modification;
    private ChatResponse originalResponse;
}
