package org.linxin.server.business.service;

public interface IEmailVerificationService {

    void sendVerificationCode(String email);

    boolean verifyCode(String email, String code);
}
