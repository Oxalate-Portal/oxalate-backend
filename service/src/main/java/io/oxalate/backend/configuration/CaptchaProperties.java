package io.oxalate.backend.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@ConfigurationProperties(prefix = "oxalate.captcha")
@Component
public class CaptchaProperties {
    private boolean enabled;
    private String verificationUrl;
    private String secretKey;
    private String siteKey;
    private float threshold;
}
