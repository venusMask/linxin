package org.venus.lin.xin.mgr.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    FAILED(500, "操作失败"),
    VALIDATE_FAILED(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有权限访问"),
    NOT_FOUND(404, "资源不存在"),
    
    // 用户相关
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    USERNAME_PASSWORD_ERROR(1003, "用户名或密码错误"),
    
    // 好友相关
    FRIEND_NOT_FOUND(2001, "好友关系不存在"),
    FRIEND_ALREADY_EXISTS(2002, "你们已经是好友了"),
    FRIEND_APPLY_NOT_FOUND(2003, "好友申请不存在"),
    CANNOT_ADD_SELF(2004, "不能添加自己为好友"),
    
    // 消息相关
    CONVERSATION_NOT_FOUND(3001, "会话不存在"),
    MESSAGE_NOT_FOUND(3002, "消息不存在");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
