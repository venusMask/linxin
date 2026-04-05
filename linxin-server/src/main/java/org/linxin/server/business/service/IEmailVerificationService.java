package org.linxin.server.business.service;

public interface IEmailVerificationService {

    void sendVerificationCode(String email);

    void sendVerificationCode(String email, String type);

    boolean verifyCode(String email, String code);

    boolean verifyCode(String email, String code, String type);
}
