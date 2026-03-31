package org.venus.lin.xin.mgr.ai.dto;

import lombok.Data;

@Data
public class ModifyParamsRequest {
    private String modification;
    private ChatResponse originalResponse;
}