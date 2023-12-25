package io.oxalate.backend.tools;

import jakarta.servlet.http.HttpServletRequest;

public class HttpTools {
    public static String getRemoteIp(HttpServletRequest request) {
        final String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            return xfHeader.split(",")[0];
        }

        return request.getRemoteAddr();
    }
}
