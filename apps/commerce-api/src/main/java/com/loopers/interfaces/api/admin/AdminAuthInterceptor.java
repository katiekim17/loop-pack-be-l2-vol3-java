package com.loopers.interfaces.api.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 어드민 API LDAP 인증 인터셉터.
 * Body 파싱 이전 단계에서 X-Loopers-Ldap 헤더를 검사하여,
 * 유효하지 않은 요청에 대해 즉시 403을 반환하고 처리를 중단한다.
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private static final String ADMIN_LDAP = "loopers.admin";
    private static final String LDAP_HEADER = "X-Loopers-Ldap";

    // Map.of()는 null 값을 허용하지 않으므로 JSON 문자열을 직접 작성한다.
    private static final String FORBIDDEN_BODY =
        "{\"meta\":{\"code\":\"Forbidden\",\"message\":\"접근 권한이 없습니다.\"},\"data\":null}";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ldap = request.getHeader(LDAP_HEADER);
        if (!ADMIN_LDAP.equals(ldap)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(FORBIDDEN_BODY);
            return false;
        }
        return true;
    }
}
