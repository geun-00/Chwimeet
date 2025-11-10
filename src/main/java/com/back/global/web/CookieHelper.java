package com.back.global.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class CookieHelper {
    private final HttpServletRequest req;
    private final HttpServletResponse resp;

    public String getCookieValue(String name, String defaultValue) {
        if(req.getCookies() == null) return defaultValue;

        return Arrays.stream(req.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(defaultValue);
    }

    public void setCookie(String name, String value) {
        boolean delete = (value == null) || value.isBlank();
        String host = req.getServerName();
        boolean isLocal = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);

        String domain = System.getenv("COOKIE_DOMAIN"); // 로컬이면 비워두기
        boolean secure = Boolean.parseBoolean(System.getenv().getOrDefault("COOKIE_SECURE",
                isLocal ? "false" : "true"));

        Cookie cookie = new Cookie(name, delete ? "" : value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setMaxAge(delete ? 0 : 60 * 60 * 24 * 365);

        // 로컬에서는 domain 지정하지 말기 (HostOnly 쿠키)
        if (!isLocal && domain != null && !domain.isBlank()) {
            cookie.setDomain(domain.startsWith(".") ? domain : domain);
        }

        resp.addCookie(cookie);
    }

    public void deleteCookie(String name) {
        setCookie(name, null);
    }
}
