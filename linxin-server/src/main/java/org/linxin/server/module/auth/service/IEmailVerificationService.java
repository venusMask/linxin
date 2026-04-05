package org.linxin.server.module.auth.service;

public interface IEmailVerificationService {

    void sendVerificationCode(String email);

    void sendVerificationCode(String email, String type);

    boolean verifyCode(String email, String code);

    boolean verifyCode(String email, String code, String type);
}
