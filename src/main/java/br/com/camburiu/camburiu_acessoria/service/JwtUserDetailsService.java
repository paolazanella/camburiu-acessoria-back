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

public class JwtUserDetailsService implements UserDetailsService{

@Autowired
    private UsuarioRepository usuarioRepository;
	
	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		Optional<Usuario> usuarioResponse = usuarioRepository.findByEmail(email);
                Usuario usuario = usuarioResponse.get();
		
		if (usuario.getEmail().equals(email)) {
			return new User(email, usuario.getSenha(),
					new ArrayList<>());
		} else {
			throw new UsernameNotFoundException("usuário não encontrado - email: " + email);
		}
	}
}