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
import org.springframework.web.bind.annotation.*;
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

    // 🔍 Listar todos os usuários (somente Admin)
    @GetMapping
    public ResponseEntity<List<Usuario>> listarUsuarios(@RequestHeader("Authorization") String token) {
        String emailUsuarioLogado = jwtTokenUtil.getUsernameFromToken(token.substring(7));
        Usuario usuarioLogado = usuarioRepository.findByEmail(emailUsuarioLogado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));

        if (usuarioLogado.getStatus() != 1) { // Apenas admin pode listar usuários
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(usuarioRepository.findAll());
    }

    // 🔍 Buscar usuário por ID (somente o próprio usuário ou admin)
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> buscarPorId(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        String emailUsuarioLogado = jwtTokenUtil.getUsernameFromToken(token.substring(7));
        Usuario usuarioLogado = usuarioRepository.findByEmail(emailUsuarioLogado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));

        Optional<Usuario> usuario = usuarioRepository.findById(id);

        // O próprio usuário ou um admin pode visualizar os detalhes
        if (usuario.isPresent() && (usuarioLogado.getStatus() == 1 || usuarioLogado.getId().equals(id))) {
            return ResponseEntity.ok(usuario.get());
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    // ➕ Criar novo usuário (usuário normal ou admin)
    @PostMapping
    public ResponseEntity<Usuario> criarUsuario(@RequestBody Usuario usuario,
            @RequestHeader("Authorization") String token) {
        String emailUsuarioLogado = jwtTokenUtil.getUsernameFromToken(token.substring(7));
        Usuario usuarioLogado = usuarioRepository.findByEmail(emailUsuarioLogado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));

        if (usuarioLogado.getStatus() != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // Apenas admin pode criar usuários
        }

        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        if (usuario.getStatus() == null) {
            usuario.setStatus(2); // Usuário comum por padrão
        }
        return ResponseEntity.ok(usuarioRepository.save(usuario));
    }

    // ✏️ Atualizar informações do usuário
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> atualizarUsuario(@PathVariable Long id, @RequestBody Usuario usuarioAtualizado,
            @RequestHeader("Authorization") String token) {
        String emailUsuarioLogado = jwtTokenUtil.getUsernameFromToken(token.substring(7));
        Usuario usuarioLogado = usuarioRepository.findByEmail(emailUsuarioLogado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));

        return usuarioRepository.findById(id).map(usuario -> {
            // Verifica se o usuário logado tem permissão para atualizar
            if (!usuarioLogado.getId().equals(id) && usuarioLogado.getStatus() != 1) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(usuario); // Retorna o usuário atual sem
                                                                                  // alterações
            }

            // Atualiza os dados do usuário
            usuario.setNome(usuarioAtualizado.getNome());
            usuario.setEmail(usuarioAtualizado.getEmail());

            // Apenas admin pode mudar status
            if (usuarioLogado.getStatus() == 1) {
                usuario.setStatus(usuarioAtualizado.getStatus());
            }

            // Salva o usuário atualizado e retorna
            return ResponseEntity.ok(usuarioRepository.save(usuario));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    // ❌ Excluir usuário (somente admin)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        String emailUsuarioLogado = jwtTokenUtil.getUsernameFromToken(token.substring(7));
        Usuario usuarioLogado = usuarioRepository.findByEmail(emailUsuarioLogado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));

        if (usuarioLogado.getStatus() != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (usuarioRepository.existsById(id)) {
            usuarioRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

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
