package org.linxin.server.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final DPUserDetailLoginService userDetailsService;
    private final org.linxin.server.business.service.IAgentTokenService agentTokenService;
    private final org.linxin.server.business.mapper.UserMapper userMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            if (token.startsWith("lx_at_")) {
                // 处理 Agent Token 鉴权
                var agentToken = agentTokenService.validateToken(token);
                if (agentToken != null) {
                    var user = userMapper.selectById(agentToken.getUserId());
                    if (user != null) {
                        setAuthentication(request, user.getUsername(), user.getId());
                    }
                }
            } else {
                // 处理标准 JWT 鉴权
                String username = jwtService.extractUsername(token);
                Long userId = jwtService.extractUserId(token);
                if (username != null && userId != null) {
                    setAuthentication(request, username, userId);
                }
            }
        } catch (Exception e) {
            logger.error("Authentication failed: " + e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    private void setAuthentication(HttpServletRequest request, String username, Long userId) {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (userDetails.isEnabled()) {
                request.setAttribute("userId", userId);
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
    }
}
