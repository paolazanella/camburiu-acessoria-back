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
        // Recupera o cabeçalho Authorization da requisição
        final String requestTokenHeader = request.getHeader("Authorization");

        if (requestTokenHeader == null || !requestTokenHeader.startsWith("Bearer ")) {
            // Se o token não for encontrado ou não começar com "Bearer ", faz log e passa
            // para o próximo filtro
            System.out.println("🚨 Nenhum token JWT recebido! Header Authorization: " + requestTokenHeader);
            chain.doFilter(request, response);
            return;
        }

        // Extrai o token JWT do cabeçalho
        String jwtToken = requestTokenHeader.substring(7);
        String username = null;

        try {
            // Tenta extrair o nome de usuário do token
            username = jwtTokenUtil.getUsernameFromToken(jwtToken);
            System.out.println("✅ Token válido. Usuário extraído do token: " + username);
        } catch (Exception e) {
            // Em caso de erro ao processar o token, retorna erro 401
            System.out.println("🚨 Erro ao processar token JWT: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "TOKEN INVÁLIDO");
            return;
        }

        // Verifica se o usuário não está autenticado e realiza a autenticação
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Carrega os detalhes do usuário com o nome extraído do token
            UserDetails userDetails = this.jwtUserDetailsService.loadUserByUsername(username);

            if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
                // Cria o token de autenticação e o coloca no contexto de segurança
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
                        null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authToken);
                System.out.println("🔐 Usuário autenticado: " + username);
            } else {
                // Se o token for inválido ou expirado
                System.out.println("🚨 Token inválido ou expirado!");
            }
        }

        // Passa a requisição para o próximo filtro
        chain.doFilter(request, response);
    }
}
