
package br.com.camburiu.camburiu_acessoria.controller;

import br.com.camburiu.camburiu_acessoria.model.Administrador;
import br.com.camburiu.camburiu_acessoria.repository.AdministradorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/administradores")
public class AdministradorController {

    private final AdministradorRepository administradorRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdministradorController(AdministradorRepository administradorRepository) {
        this.administradorRepository = administradorRepository;
    }

    // ✅ Somente Administradores podem listar todos os administradores
    @GetMapping
    public List<Administrador> listarAdministradores() {
        return administradorRepository.findAll();
    }

    // ✅ Somente Administradores podem visualizar outro administrador
    @GetMapping("/{id}")
    public ResponseEntity<Administrador> buscarPorId(@PathVariable Long id) {
        Optional<Administrador> administrador = administradorRepository.findById(id);
        return administrador.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ✅ Somente Administradores podem criar outros Administradores
    @PostMapping
    public ResponseEntity<Administrador> criarAdministrador(@RequestBody Administrador administrador) {
        administrador.setSenha(passwordEncoder.encode(administrador.getSenha())); // Senha criptografada
        return ResponseEntity.ok(administradorRepository.save(administrador));
    }
    
}