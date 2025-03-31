package br.com.camburiu.camburiu_acessoria.controller;
import lombok.extern.slf4j.Slf4j;
import br.com.camburiu.camburiu_acessoria.model.Veiculo;
import br.com.camburiu.camburiu_acessoria.repository.VeiculoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j // Esta anotação cria automaticamente o logger
@RestController
@RequestMapping("/veiculos")
public class VeiculoController {

    @Autowired
    private VeiculoRepository veiculoRepository;

    @GetMapping
    public List<Veiculo> listarVeiculos() {
        return veiculoRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Veiculo> buscarPorId(@PathVariable Long id) {
        Optional<Veiculo> veiculo = veiculoRepository.findById(id);
        return veiculo.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }



    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarVeiculo(@PathVariable Long id) {
        if (veiculoRepository.existsById(id)) {
            veiculoRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarVeiculo(
            @PathVariable Long id,
            @RequestBody Veiculo veiculoAtualizado) {

        // Log detalhado para depuração
        log.info("Tentativa de atualização do veículo ID: {}", id);
        log.debug("Dados recebidos para atualização: {}", veiculoAtualizado);

        // Verifica se o veículo existe
        Optional<Veiculo> veiculoOpt = veiculoRepository.findById(id);

        if (veiculoOpt.isEmpty()) {
            log.warn("Veículo não encontrado para ID: {}", id);
            log.info("IDs de veículos existentes: {}",
                    veiculoRepository.findAll().stream().map(Veiculo::getId).collect(Collectors.toList()));
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Veículo não encontrado com ID: " + id);
        }

        // Validações
        if (veiculoAtualizado.getPlaca() == null || veiculoAtualizado.getPlaca().isEmpty()) {
            return ResponseEntity.badRequest().body("Placa é obrigatória");
        }

        if (veiculoAtualizado.getRenavam() == null || veiculoAtualizado.getRenavam().isEmpty()) {
            return ResponseEntity.badRequest().body("Renavam é obrigatório");
        }

        // Atualização
        Veiculo veiculo = veiculoOpt.get();
        veiculo.setPlaca(veiculoAtualizado.getPlaca());
        veiculo.setRenavam(veiculoAtualizado.getRenavam());

        try {
            Veiculo veiculoSalvo = veiculoRepository.save(veiculo);
            log.info("Veículo atualizado com sucesso: {}", veiculoSalvo);
            return ResponseEntity.ok(veiculoSalvo);
        } catch (Exception e) {
            log.error("Erro ao atualizar veículo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao atualizar veículo");
        }
    }

    @PostMapping
    public ResponseEntity<?> criarVeiculo(@RequestBody Veiculo veiculo) {
        if (veiculo.getPlaca() == null || veiculo.getPlaca().isEmpty()) {
            return ResponseEntity.badRequest().body("Placa é obrigatória");
        }
        if (veiculo.getCliente() == null || veiculo.getCliente().getId() == null) {
            return ResponseEntity.badRequest().body("Cliente é obrigatório");
        }

        try {
            veiculo.setVencimento(this.calcularVencimento(veiculo.getPlaca()));
            return ResponseEntity.ok(veiculoRepository.save(veiculo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Método protegido para poder ser acessado pelo ClienteController
    protected Date calcularVencimento(String placa) {
        char ultimoChar = placa.toUpperCase().charAt(placa.length() - 1);
        int digito = Character.getNumericValue(ultimoChar);

        if (digito < 0 || digito > 9) {
            throw new IllegalArgumentException("Final da placa deve ser numérico");
        }

        int mes = digito == 0 ? 10 : digito;
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, mes - 1);
        calendar.set(Calendar.DAY_OF_MONTH, 10);

        if (calendar.getTime().before(new Date())) {
            calendar.add(Calendar.YEAR, 1);
        }

        return calendar.getTime();
    }
}