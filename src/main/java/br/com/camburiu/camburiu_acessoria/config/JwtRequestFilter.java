package br.com.camburiu.camburiu_acessoria.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import br.com.camburiu.camburiu_acessoria.service.JwtUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUserDetailsService jwtUserDetailsService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain)
            throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");

        System.out.println("🔍 Token recebido no cabeçalho: " + requestTokenHeader); // DEBUG

        if (requestTokenHeader == null || !requestTokenHeader.startsWith("Bearer ")) {
            System.out.println("🚨 1--Nenhum token JWT válido recebido! Header Authorization: " + requestTokenHeader);
            chain.doFilter(request, response);
            return;
        }

        // Extrai o token JWT removendo "Bearer "
        String jwtToken = requestTokenHeader.substring(7);
        String username = null;

        try {
            username = jwtTokenUtil.getUsernameFromToken(jwtToken);
            System.out.println("✅ Token válido. Usuário extraído do token: " + username);
        } catch (Exception e) {
            System.out.println("🚨 Erro ao processar token JWT: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "TOKEN INVÁLIDO");
            return;
        }

        // Se o usuário for válido, autentica no contexto do Spring Security
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.jwtUserDetailsService.loadUserByUsername(username);

            if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
                        null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authToken);
                System.out.println("🔐 Usuário autenticado: " + username);
            } else {
                System.out.println("🚨 Token inválido ou expirado!");
            }
        }

        chain.doFilter(request, response);
    }
}
