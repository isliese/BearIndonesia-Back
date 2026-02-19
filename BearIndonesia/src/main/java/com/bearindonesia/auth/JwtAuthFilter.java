package com.bearindonesia.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * 공개(permitAll) 엔드포인트는 JWT 필터를 아예 스킵한다.
     * 이렇게 하면 permitAll인데도 403이 나는 "필터/체인 꼬임"을 원천 차단할 수 있음.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();

        // Preflight
        if (HttpMethod.OPTIONS.matches(method)) return true;

        // Public endpoints
        if (path.startsWith("/auth/")) return true;
        if (path.startsWith("/ingest/")) return true;

        // Public GET APIs
        if (HttpMethod.GET.matches(method)) {
            if (path.startsWith("/api/news")) return true;
            if (path.startsWith("/api/articles")) return true;
            if (path.startsWith("/api/newsletter")) return true;
        }

        // Public POST APIs
        if (HttpMethod.POST.matches(method)) {
            if (path.equals("/api/search")) return true;
            if (path.equals("/api/wordcloud")) return true;
        }

        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                AuthUser user = jwtService.parseToken(token);
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name()));
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}