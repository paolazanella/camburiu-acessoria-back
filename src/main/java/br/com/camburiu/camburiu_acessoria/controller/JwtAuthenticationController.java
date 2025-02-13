/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package br.com.camburiu.camburiu_acessoria.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.com.camburiu.camburiu_acessoria.config.JwtTokenUtil;
import br.com.camburiu.camburiu_acessoria.model.JwtRequest;
import br.com.camburiu.camburiu_acessoria.model.JwtResponse;
import br.com.camburiu.camburiu_acessoria.service.JwtUserDetailsService;

@RestController
@CrossOrigin
public class JwtAuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private JwtUserDetailsService userDetailsService;

    @PostMapping("/api/authenticate")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody JwtRequest authenticationRequest) {
        try {
            System.out.println("🔍 Iniciando autenticação para: " + authenticationRequest.getUsername());

            authenticate(authenticationRequest.getUsername(), authenticationRequest.getPassword());

            final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getUsername());
            final String token = jwtTokenUtil.generateToken(userDetails);

            System.out.println("✅ Token gerado com sucesso!");
            return ResponseEntity.ok(new JwtResponse(token));
        } catch (Exception e) {
            System.out.println("❌ Erro ao autenticar: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USUARIO_INCORRETO");
        }
    }

    private void authenticate(String username, String password) throws Exception {
        try {
            System.out.println("🔑 Autenticando: " + username);

            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));

            System.out.println("✅ Autenticação bem-sucedida para: " + username);
        } catch (DisabledException e) {
            System.out.println("❌ Erro: Usuário desativado!");
            throw new Exception("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            System.out.println("❌ Erro: Credenciais inválidas!");
            throw new Exception("INVALID_CREDENTIALS", e);
        }
    }
}
