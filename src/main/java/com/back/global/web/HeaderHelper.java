package com.back.global.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HeaderHelper {
    private final HttpServletRequest req;
    private final HttpServletResponse resp;

    public void setHeader(String name, String value) {
        boolean delete = (value == null) || value.isBlank();
        String host = req.getServerName();
        boolean isLocal = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);

        String domain = System.getenv("COOKIE_DOMAIN"); // 로컬이면 비워두기
        boolean secure = Boolean.parseBoolean(System.getenv().getOrDefault("COOKIE_SECURE",
                isLocal ? "false" : "true"));
        boolean crossSite = Boolean.parseBoolean(System.getenv().getOrDefault("COOKIE_CROSS_SITE",
                isLocal ? "false" : "true")); // prod: true

        // 2) SameSite는 컨테이너별 편차가 있으므로 Set-Cookie 헤더로 확정
        String sameSite = crossSite ? "None" : "Lax"; // cross-site면 무조건 None
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(delete ? "" : value)
                .append("; Path=/")
                .append("; Max-Age=").append(delete ? 0 : 60 * 60 * 24 * 365)
                .append("; HttpOnly");
        if (!isLocal && domain != null && !domain.isBlank()) sb.append("; Domain=").append(domain);
        if (secure) sb.append("; Secure");
        sb.append("; SameSite=").append(sameSite);

        resp.addHeader("Set-Cookie", sb.toString());
    }
}
