package org.linxin.server.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDateTime;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.linxin.server.business.entity.EmailVerificationCode;
import org.linxin.server.business.mapper.EmailVerificationCodeMapper;
import org.linxin.server.business.service.IEmailVerificationService;
import org.linxin.server.common.exception.BusinessException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements IEmailVerificationService {

    private final EmailVerificationCodeMapper emailVerificationCodeMapper;
    private final JavaMailSender mailSender;

    private static final int CODE_LENGTH = 6;
    private static final int CODE_EXPIRE_MINUTES = 5;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void sendVerificationCode(String email) {
        String code = generateCode();

        LambdaUpdateWrapper<EmailVerificationCode> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(EmailVerificationCode::getEmail, email)
                .set(EmailVerificationCode::getStatus, 2);
        emailVerificationCodeMapper.update(null, updateWrapper);

        EmailVerificationCode verificationCode = new EmailVerificationCode();
        verificationCode.setEmail(email);
        verificationCode.setCode(code);
        verificationCode.setType("register");
        verificationCode.setStatus(0);
        verificationCode.setExpireTime(LocalDateTime.now().plusMinutes(CODE_EXPIRE_MINUTES));
        emailVerificationCodeMapper.insert(verificationCode);

        sendEmail(email, code);
    }

    @Override
    public boolean verifyCode(String email, String code) {
        LambdaQueryWrapper<EmailVerificationCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmailVerificationCode::getEmail, email)
                .eq(EmailVerificationCode::getCode, code)
                .eq(EmailVerificationCode::getStatus, 0)
                .orderByDesc(EmailVerificationCode::getCreateTime)
                .last("LIMIT 1");

        EmailVerificationCode verificationCode = emailVerificationCodeMapper.selectOne(wrapper);

        if (verificationCode == null) {
            throw new BusinessException("验证码错误");
        }

        if (verificationCode.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("验证码已过期");
        }

        verificationCode.setStatus(1);
        emailVerificationCodeMapper.updateById(verificationCode);

        return true;
    }

    private String generateCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    private void sendEmail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("灵信注册验证码");
        message.setText("您的验证码是：" + code + "，有效期5分钟，请勿泄露给他人。");
        mailSender.send(message);
    }
}
