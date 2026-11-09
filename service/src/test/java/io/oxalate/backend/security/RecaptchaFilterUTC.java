package io.oxalate.backend.security;

import static io.oxalate.backend.api.UrlConstants.API;
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.service.RecaptchaService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RecaptchaFilterUTC {

    @Test
    void googleVerificationFailureReturnsServiceUnavailableResponse() throws Exception {
        var recaptchaService = mock(RecaptchaService.class);
        when(recaptchaService.isCaptchaEnabled()).thenReturn(true);
        when(recaptchaService.validateToken("captcha-token")).thenThrow(new RuntimeException("Google unavailable"));
        var filter = new RecaptchaFilter(recaptchaService, mock(AppEventPublisher.class));
        var request = new MockHttpServletRequest("POST", API + "/auth/login");
        request.addHeader("X-Captcha-Token", "captcha-token");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(503, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals("{\"code\":\"" + RecaptchaFilter.RECAPTCHA_VERIFICATION_FAILED + "\"}", response.getContentAsString());
        assertNull(chain.getRequest(), "The request must not continue after Google verification fails");
        verify(recaptchaService).validateToken("captcha-token");
    }
}
