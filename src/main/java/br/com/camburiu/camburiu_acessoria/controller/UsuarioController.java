package br.com.camburiu.camburiu_acessoria.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.com.camburiu.camburiu_acessoria.config.JwtTokenUtil;
import br.com.camburiu.camburiu_acessoria.model.JwtResponse;
import br.com.camburiu.camburiu_acessoria.model.Usuario;
import br.com.camburiu.camburiu_acessoria.repository.UsuarioRepository;
import br.com.camburiu.camburiu_acessoria.service.JwtUserDetailsService;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private JwtUserDetailsService userDetailsService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 🔐 Endpoint para login e geração de Token
    @PostMapping("/authenticate")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody Usuario usuario) {
        try {
            authenticate(usuario.getEmail(), usuario.getSenha());

            final UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getEmail());
            final String token = jwtTokenUtil.generateToken(userDetails);

            return ResponseEntity.ok(new JwtResponse(token));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USUARIO_INCORRETO");
        }
    }

    // 🔍 Listar todos os usuários (apenas para admin)
    @GetMapping
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    // 🔍 Buscar usuário por ID (somente o próprio usuário ou admin)
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> buscarPorId(@PathVariable Long id) {
        Optional<Usuario> usuario = usuarioRepository.findById(id);
        return usuario.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ➕ Criar novo usuário e já logar ele
    @PostMapping("/criar")
    public ResponseEntity<?> criarUsuario(@RequestBody Usuario usuario) {
        try {
            usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
            Usuario novoUsuario = usuarioRepository.save(usuario);

            // 🔐 Após salvar, já faz login e retorna token
            authenticate(novoUsuario.getEmail(), usuario.getSenha());
            final UserDetails userDetails = userDetailsService.loadUserByUsername(novoUsuario.getEmail());
            final String token = jwtTokenUtil.generateToken(userDetails);

            return ResponseEntity.ok(new JwtResponse(token));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "EMAIL_DUPLICADO");
        }
    }

    // ✏️ Atualizar informações do usuário (somente ele pode)
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> atualizarUsuario(@PathVariable Long id, @RequestBody Usuario usuarioAtualizado) {
        return usuarioRepository.findById(id)
                .map(usuario -> {
                    usuario.setNome(usuarioAtualizado.getNome());
                    usuario.setEmail(usuarioAtualizado.getEmail());
                    usuario.setSenha(passwordEncoder.encode(usuarioAtualizado.getSenha())); // Senha criptografada
                    return ResponseEntity.ok(usuarioRepository.save(usuario));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ❌ Excluir usuário (somente admin)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable Long id) {
        if (usuarioRepository.existsById(id)) {
            usuarioRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // 🔐 Método privado para autenticar usuário
    private void authenticate(String username, String password) throws Exception {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (DisabledException e) {
            throw new Exception("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            throw new Exception("INVALID_CREDENTIALS", e);
        }
    }

}
