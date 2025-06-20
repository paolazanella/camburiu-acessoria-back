/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package br.com.camburiu.camburiu_acessoria.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "veiculos")
public class Veiculo {




    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 7)
    private String placa;

    @Column(nullable = false, unique = true, length = 9)
    private String renavam;

    @Column(nullable = false)
    @Temporal(TemporalType.DATE)
    private Date vencimento;

    @ManyToOne(optional = false)  // Torna o cliente obrigatório
    @JsonBackReference  // Lado inverso da relação
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;
}