package com.campus.task.config;

import com.campus.task.common.utils.JwtUtil;
import com.campus.task.module.user.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token)) {
            Long userId = jwtUtil.getUserId(token);
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                boolean allowByRedis = false;
                try {
                    // 比对 Redis 中的 Token（防止登出后重用）；
                    // 若本地未启动 Redis 或连接异常，降级为仅校验 JWT（便于本地开发）。
                    String redisToken = redisTemplate.opsForValue().get("token:" + userId);
                    allowByRedis = (redisToken == null) || token.equals(redisToken);
                } catch (Exception ex) {
                    // Redis 不可用时，允许基于 JWT 继续（开发友好）；生产环境应确保 Redis 可用
                    allowByRedis = true;
                }

                if (allowByRedis) {
                    UserDetails userDetails = userDetailsService.loadUserById(userId);
                    if (userDetails != null) {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            }
        }
        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
