/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package br.com.camburiu.camburiu_acessoria.controller;

import br.com.camburiu.camburiu_acessoria.model.Cliente;
import br.com.camburiu.camburiu_acessoria.model.Veiculo;
import br.com.camburiu.camburiu_acessoria.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/clientes")
public class ClienteController {
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private VeiculoController veiculoController;
    @GetMapping
    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cliente> buscarPorId(@PathVariable Long id) {
        Optional<Cliente> cliente = clienteRepository.findById(id);
        return cliente.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> criarCliente(@RequestBody Cliente cliente) {
        if (cliente.getVeiculos() != null) {
            for (Veiculo veiculo : cliente.getVeiculos()) {
                if (veiculo.getPlaca() == null) {
                    return ResponseEntity.badRequest().body("Placa é obrigatória");
                }
                veiculo.setVencimento(
                        veiculoController.calcularVencimento(veiculo.getPlaca())
                );
            }
        }
        return ResponseEntity.ok(clienteRepository.save(cliente));
    }



    @PutMapping("/{id}")
    public ResponseEntity<Cliente> atualizarCliente(@PathVariable Long id, @RequestBody Cliente clienteAtualizado) {
        return clienteRepository.findById(id)
                .map(cliente -> {
                    cliente.setNome(clienteAtualizado.getNome());
                    cliente.setCpfOuCnpj(clienteAtualizado.getCpfOuCnpj());
                    cliente.setTelefone(clienteAtualizado.getTelefone());
                    return ResponseEntity.ok(clienteRepository.save(cliente));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarCliente(@PathVariable Long id) {
        if (clienteRepository.existsById(id)) {
            clienteRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
