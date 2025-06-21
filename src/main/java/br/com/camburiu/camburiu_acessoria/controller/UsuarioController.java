package br.com.camburiu.camburiu_acessoria.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import br.com.camburiu.camburiu_acessoria.config.JwtTokenUtil;

import br.com.camburiu.camburiu_acessoria.model.Usuario;
import br.com.camburiu.camburiu_acessoria.repository.UsuarioRepository;
import br.com.camburiu.camburiu_acessoria.service.JwtUserDetailsService;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final JwtTokenUtil jwtTokenUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 🔹 Injeção de dependências pelo construtor
    public UsuarioController(UsuarioRepository usuarioRepository,
            JwtTokenUtil jwtTokenUtil,
            JwtUserDetailsService userDetailsService,
            AuthenticationManager authenticationManager) {
        this.usuarioRepository = usuarioRepository;
        this.jwtTokenUtil = jwtTokenUtil;

    }

    // 🔍 Listar todos os usuários (somente Admin)
    @GetMapping
    public ResponseEntity<List<Usuario>> listarUsuarios(
            @RequestHeader(value = "Authorization", required = false) String token) {
        Usuario usuarioLogado = validarTokenEObterUsuario(token);
        if (usuarioLogado.getStatus() != 1) { // Apenas admin pode listar usuários
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(usuarioRepository.findAll());
    }

    // 🔍 Buscar usuário por ID (somente o próprio usuário ou admin)
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> buscarPorId(@PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        Usuario usuarioLogado = validarTokenEObterUsuario(token);
        Optional<Usuario> usuario = usuarioRepository.findById(id);

        if (usuario.isPresent() && (usuarioLogado.getStatus() == 1 || usuarioLogado.getId().equals(id))) {
            return ResponseEntity.ok(usuario.get());
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
    @PostMapping
    // ➕ Criar novo usuário (somente Admin, exceto no primeiro cadastro)
    public ResponseEntity<Usuario> criarUsuario(@RequestBody Usuario usuario, Authentication authentication) {
        long totalUsuarios = usuarioRepository.count();

        if (totalUsuarios == 0) {
            usuario.setStatus(1); // ✅ Primeiro usuário se torna ADMIN automaticamente
        } else {

                // 🔥 Verifica se existe pelo menos um ADMIN cadastrado antes de criar novos usuários
                boolean existeAdmin = usuarioRepository.existsByStatus(1);
                if (!existeAdmin) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nenhum admin cadastrado.");
                }

                // 🔍 Pega o usuário logado
                Usuario usuarioLogado = usuarioRepository.findByEmail(authentication.getName())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado"));

                // ⚠️ Somente ADMIN pode criar novos usuários
                if (usuarioLogado.getStatus() != 1) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas administradores podem criar novos usuários.");
                }
            }


        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        return ResponseEntity.ok(usuarioRepository.save(usuario));
    }

    // ✏️ Atualizar informações do usuário
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> atualizarUsuario(@PathVariable Long id, @RequestBody Usuario usuarioAtualizado,
            @RequestHeader(value = "Authorization", required = false) String token) {
        Usuario usuarioLogado = validarTokenEObterUsuario(token);

        return usuarioRepository.findById(id).map(usuario -> {
            if (!usuarioLogado.getId().equals(id) && usuarioLogado.getStatus() != 1) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(usuario);
            }

            usuario.setNome(usuarioAtualizado.getNome());
            usuario.setEmail(usuarioAtualizado.getEmail());

            if (usuarioLogado.getStatus() == 1) { // Apenas admin pode mudar status
                usuario.setStatus(usuarioAtualizado.getStatus());
            }

            return ResponseEntity.ok(usuarioRepository.save(usuario));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    // ❌ Excluir usuário (somente admin)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        Usuario usuarioLogado = validarTokenEObterUsuario(token);
        if (usuarioLogado.getStatus() != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (usuarioRepository.existsById(id)) {
            usuarioRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // 🔑 Método para validar o token e obter usuário autenticado
    private Usuario validarTokenEObterUsuario(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "TOKEN INVÁLIDO");
        }

        String emailUsuarioLogado = jwtTokenUtil.getUsernameFromToken(token.substring(7));
        return usuarioRepository.findByEmail(emailUsuarioLogado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));
    }

    // 👤 OBTER PERFIL ATUAL
    @GetMapping("/me")
    public ResponseEntity<Usuario> obterPerfilAtual(
            @RequestHeader(value = "Authorization", required = false) String token) {
        Usuario usuarioLogado = validarTokenEObterUsuario(token);

        // Criar usuário sem senha para retornar
        Usuario perfilSeguro = new Usuario();
        perfilSeguro.setId(usuarioLogado.getId());
        perfilSeguro.setNome(usuarioLogado.getNome());
        perfilSeguro.setEmail(usuarioLogado.getEmail());
        perfilSeguro.setStatus(usuarioLogado.getStatus());

        return ResponseEntity.ok(perfilSeguro);
    }

    // 🔒 ALTERAR SENHA
    @PutMapping("/{id}/senha")
    public ResponseEntity<?> alterarSenha(
            @PathVariable Long id,
            @RequestBody AlterarSenhaRequest request,
            @RequestHeader(value = "Authorization", required = false) String token) {

        Usuario usuarioLogado = validarTokenEObterUsuario(token);

        if (!usuarioLogado.getId().equals(id) && usuarioLogado.getStatus() != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Sem permissão para alterar senha");
        }

        return usuarioRepository.findById(id).map(usuario -> {
            if (usuarioLogado.getId().equals(id)) {
                if (!passwordEncoder.matches(request.getSenhaAtual(), usuario.getSenha())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Senha atual incorreta");
                }
            }

            if (request.getNovaSenha() == null || request.getNovaSenha().length() < 6) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Nova senha deve ter pelo menos 6 caracteres");
            }

            usuario.setSenha(passwordEncoder.encode(request.getNovaSenha()));
            usuarioRepository.save(usuario);

            return ResponseEntity.ok().body("Senha alterada com sucesso");
        }).orElse(ResponseEntity.notFound().build());
    }

    // 📋 CLASSE PARA ALTERAR SENHA
    public static class AlterarSenhaRequest {
        private String senhaAtual;
        private String novaSenha;

        public String getSenhaAtual() { return senhaAtual; }
        public void setSenhaAtual(String senhaAtual) { this.senhaAtual = senhaAtual; }

        public String getNovaSenha() { return novaSenha; }
        public void setNovaSenha(String novaSenha) { this.novaSenha = novaSenha; }
    }
}
