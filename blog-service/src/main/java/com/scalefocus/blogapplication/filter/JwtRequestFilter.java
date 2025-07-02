package com.scalefocus.blogapplication.filter;

import com.scalefocus.blogapplication.security.JwtUserDetails;
import com.scalefocus.blogapplication.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) {
        try {
            final String authHeader = request.getHeader("Authorization");
            final String jwtToken;
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }
            jwtToken = authHeader.substring(7);

            Claims claims = jwtUtil.validateToken(jwtToken);
            if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                String username = claims.getSubject();
                Long userId = claims.get("user_id", Long.class);
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();

                // Extract roles from claims if available
                String roles = (String) claims.get("roles");
                if (roles != null) {
                    for (String role : roles.split(",")) {
                        authorities.add(new SimpleGrantedAuthority(role));
                    }
                }

                JwtUserDetails userDetails = new JwtUserDetails(userId, username, authorities);

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, authorities);

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            handlerExceptionResolver.resolveException(request, response, null, e);
        }
    }
}
