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

    // 👤 OBTER PERFIL ATUAL - ENDPOINT PRINCIPAL PARA O FRONTEND
    @GetMapping("/me")
    public ResponseEntity<Usuario> obterPerfilAtual(Authentication authentication) {
        try {
            System.out.println("🔍 [/me] Obtendo perfil para usuário: " + authentication.getName());

            // Busca o usuário pelo email do token
            Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

            System.out.println("✅ [/me] Usuário encontrado: " + usuario.getNome() + " (ID: " + usuario.getId() + ")");

            // Criar resposta sem senha para segurança
            Usuario perfilSeguro = new Usuario();
            perfilSeguro.setId(usuario.getId());
            perfilSeguro.setNome(usuario.getNome());
            perfilSeguro.setEmail(usuario.getEmail());
            perfilSeguro.setStatus(usuario.getStatus());

            System.out.println("✅ [/me] Perfil retornado com sucesso");

            return ResponseEntity.ok(perfilSeguro);
        } catch (Exception e) {
            System.out.println("❌ [/me] Erro ao obter perfil: " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao carregar perfil: " + e.getMessage());
        }
    }

    // 🔍 Listar todos os usuários (somente Admin)
    @GetMapping
    public ResponseEntity<List<Usuario>> listarUsuarios(Authentication authentication) {
        try {
            System.out.println("👥 [/usuarios] Listando usuários para: " + authentication.getName());

            Usuario usuarioLogado = usuarioRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado"));

            if (usuarioLogado.getStatus() != 1) { // Apenas admin pode listar usuários
                System.out.println("❌ [/usuarios] Acesso negado - usuário não é admin");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<Usuario> usuarios = usuarioRepository.findAll();
            System.out.println("✅ [/usuarios] " + usuarios.size() + " usuários encontrados");

            return ResponseEntity.ok(usuarios);
        } catch (Exception e) {
            System.out.println("❌ [/usuarios] Erro ao listar usuários: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao listar usuários");
        }
    }

    // 🔍 Buscar usuário por ID (somente o próprio usuário ou admin)
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> buscarPorId(@PathVariable Long id, Authentication authentication) {
        try {
            System.out.println("🔍 [/usuarios/" + id + "] Buscando usuário para: " + authentication.getName());

            Usuario usuarioLogado = usuarioRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado"));

            Optional<Usuario> usuario = usuarioRepository.findById(id);

            if (usuario.isPresent() && (usuarioLogado.getStatus() == 1 || usuarioLogado.getId().equals(id))) {
                Usuario usuarioEncontrado = usuario.get();

                // Criar resposta sem senha
                Usuario usuarioSeguro = new Usuario();
                usuarioSeguro.setId(usuarioEncontrado.getId());
                usuarioSeguro.setNome(usuarioEncontrado.getNome());
                usuarioSeguro.setEmail(usuarioEncontrado.getEmail());
                usuarioSeguro.setStatus(usuarioEncontrado.getStatus());

                return ResponseEntity.ok(usuarioSeguro);
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            System.out.println("❌ [/usuarios/" + id + "] Erro: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao buscar usuário");
        }
    }

    // ➕ Criar novo usuário (somente Admin, exceto no primeiro cadastro)
    @PostMapping
    public ResponseEntity<Usuario> criarUsuario(@RequestBody Usuario usuario, Authentication authentication) {
        try {
            System.out.println("➕ [POST /usuarios] Criando usuário: " + usuario.getEmail());

            long totalUsuarios = usuarioRepository.count();

            if (totalUsuarios == 0) {
                usuario.setStatus(1); // ✅ Primeiro usuário se torna ADMIN automaticamente
                System.out.println("✅ [POST /usuarios] Primeiro usuário - definindo como ADMIN");
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
            Usuario usuarioSalvo = usuarioRepository.save(usuario);

            System.out.println("✅ [POST /usuarios] Usuário criado: " + usuarioSalvo.getEmail());

            return ResponseEntity.ok(usuarioSalvo);
        } catch (Exception e) {
            System.out.println("❌ [POST /usuarios] Erro: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao criar usuário: " + e.getMessage());
        }
    }

    // ✏️ Atualizar informações do usuário
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> atualizarUsuario(@PathVariable Long id, @RequestBody Usuario usuarioAtualizado,
                                                    Authentication authentication) {
        try {
            System.out.println("✏️ [PUT /usuarios/" + id + "] Atualizando usuário para: " + authentication.getName());

            Usuario usuarioLogado = usuarioRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado"));

            return usuarioRepository.findById(id).map(usuario -> {
                if (!usuarioLogado.getId().equals(id) && usuarioLogado.getStatus() != 1) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão para atualizar este usuário");
                }

                usuario.setNome(usuarioAtualizado.getNome());
                usuario.setEmail(usuarioAtualizado.getEmail());

                if (usuarioLogado.getStatus() == 1) { // Apenas admin pode mudar status
                    usuario.setStatus(usuarioAtualizado.getStatus());
                }

                Usuario usuarioSalvo = usuarioRepository.save(usuario);
                System.out.println("✅ [PUT /usuarios/" + id + "] Usuário atualizado");

                return ResponseEntity.ok(usuarioSalvo);
            }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        } catch (Exception e) {
            System.out.println("❌ [PUT /usuarios/" + id + "] Erro: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao atualizar usuário");
        }
    }

    // ❌ Excluir usuário (somente admin)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable Long id, Authentication authentication) {
        try {
            System.out.println("🗑️ [DELETE /usuarios/" + id + "] Deletando usuário para: " + authentication.getName());

            Usuario usuarioLogado = usuarioRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado"));

            if (usuarioLogado.getStatus() != 1) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if (usuarioRepository.existsById(id)) {
                usuarioRepository.deleteById(id);
                System.out.println("✅ [DELETE /usuarios/" + id + "] Usuário deletado");
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.out.println("❌ [DELETE /usuarios/" + id + "] Erro: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao deletar usuário");
        }
    }

    // 🔒 ALTERAR SENHA
    @PutMapping("/{id}/senha")
    public ResponseEntity<?> alterarSenha(
            @PathVariable Long id,
            @RequestBody AlterarSenhaRequest request,
            Authentication authentication) {

        try {
            System.out.println("🔒 [PUT /usuarios/" + id + "/senha] Alterando senha para: " + authentication.getName());

            Usuario usuarioLogado = usuarioRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado"));

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

                System.out.println("✅ [PUT /usuarios/" + id + "/senha] Senha alterada com sucesso");
                return ResponseEntity.ok().body("Senha alterada com sucesso");
            }).orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            System.out.println("❌ [PUT /usuarios/" + id + "/senha] Erro: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao alterar senha");
        }
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