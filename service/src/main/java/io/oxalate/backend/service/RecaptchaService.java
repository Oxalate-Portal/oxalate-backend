package io.oxalate.backend.service;

import io.oxalate.backend.client.api.response.RecaptchaResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RequiredArgsConstructor
@Service
public class RecaptchaService {

    private final RestTemplate restTemplate;

    @Value("${oxalate.captcha.enabled}")
    private boolean captchaEnabled;

    @Value("${oxalate.captcha.verification-url}")
    private String captchaVerificationUrl;

    @Value("${oxalate.captcha.site-key}")
    private String captchaSiteKey;

    @Value("${oxalate.captcha.secret-key}")
    private String captchaSecretKey;

    @Getter
    @Value("${oxalate.captcha.threshold}")
    private float captchaThreshold;

    public RecaptchaResponse validateToken(String recaptchaToken) {
        if (!captchaEnabled) {
            return null;
        }

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        var headerMap = new LinkedMultiValueMap<String, String>();

        headerMap.add("secret", captchaSecretKey);
        headerMap.add("response", recaptchaToken);

        var entity = new HttpEntity<MultiValueMap<String, String>>(headerMap, headers);

        var response = restTemplate.exchange(captchaVerificationUrl, HttpMethod.POST, entity, RecaptchaResponse.class);

        if (!response.getStatusCode()
                     .equals(HttpStatus.OK) || response.getBody() == null) {
            log.error("Could not validate captcha token against Google, got {} as response and body: {}", response.getStatusCode(), response.getBody());
            throw new RuntimeException("Failed to validate captcha token against Google");
        }

        return response.getBody();
    }
}
