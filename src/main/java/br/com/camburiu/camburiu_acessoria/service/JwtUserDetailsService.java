/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package br.com.camburiu.camburiu_acessoria.service;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import br.com.camburiu.camburiu_acessoria.model.Usuario;
import br.com.camburiu.camburiu_acessoria.repository.UsuarioRepository;

@Service
public class JwtUserDetailsService implements UserDetailsService{

@Autowired
    private UsuarioRepository usuarioRepository;
	
	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		System.out.println("🔎 Buscando usuário pelo email: " + email);
		
		Optional<Usuario> usuarioResponse = usuarioRepository.findByEmail(email);
	
		if (usuarioResponse.isEmpty()) {
			System.out.println("❌ Erro: Usuário não encontrado - " + email);
			throw new UsernameNotFoundException("Usuário não encontrado: " + email);
		}
	
		Usuario usuario = usuarioResponse.get();
		System.out.println("🔍 Usuário encontrado: " + usuario.getEmail());
	
		return new User(usuario.getEmail(), usuario.getSenha(), new ArrayList<>());
	}
}