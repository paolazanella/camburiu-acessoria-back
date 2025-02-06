/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package br.com.camburiu.camburiu_acessoria.repository;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.camburiu.camburiu_acessoria.model.Administrador;

import java.util.Optional;

public interface AdministradorRepository extends JpaRepository<Administrador, Long> {
    Optional<Administrador> findByEmail(String email);
}
