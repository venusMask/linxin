package org.linxin.server.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "auth")
public class AuthConfig {

    private String secret = "default_secret_key_for_jwt_token_generation_that_is_at_least_256_bits_long"; 
    
    private int expire;

    public int getExpire() {
        return expire;
    }

    public void setExpire(int expire) {
        this.expire = expire;
    }

    public long getExpireInMillis() {
        return (long) expire * 60 * 1000;
    }

    public String getSecret() {
        // 确保密钥长度至少为 256 位（32 个字符）
        if (secret == null || secret.length() < 32) {
            return "default_secret_key_for_jwt_token_generation_that_is_at_least_256_bits_long";
        }
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
