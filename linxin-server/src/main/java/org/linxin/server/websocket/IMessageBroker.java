package org.linxin.server.websocket;

/**
 * 跨节点消息经纪人接口
 */
public interface IMessageBroker {
    /**
     * 将消息发送给特定用户，可能在本地节点，也可能在远程节点
     */
    void broadcastToUser(Long userId, Object message);
}
