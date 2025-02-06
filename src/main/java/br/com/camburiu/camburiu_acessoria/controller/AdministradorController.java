/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package br.com.camburiu.camburiu_acessoria.controller;

import br.com.camburiu.camburiu_acessoria.model.Administrador;
import br.com.camburiu.camburiu_acessoria.repository.AdministradorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/administradores")
public class AdministradorController {

    @Autowired
    private AdministradorRepository administradorRepository;

    @GetMapping
    public List<Administrador> listarAdministradores() {
        return administradorRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Administrador> buscarPorId(@PathVariable Long id) {
        Optional<Administrador> administrador = administradorRepository.findById(id);
        return administrador.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Administrador criarAdministrador(@RequestBody Administrador administrador) {
        return administradorRepository.save(administrador);
    }
}