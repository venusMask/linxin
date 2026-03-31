package org.venus.lin.xin.mgr.websocket;

import lombok.Data;

@Data
public class WebSocketMessage {
    private String type;
    private Object data;
    private Long timestamp;

    public WebSocketMessage(String type, Object data) {
        this.type = type;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
}
