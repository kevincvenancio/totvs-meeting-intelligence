package br.com.totvs.insight360.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Representa um insight extraído da análise de uma reunião.
 */
@Entity
@Table(name = "insights")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Insight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reuniao_id")
    private Reuniao reuniao;

    private String tipo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    private String prioridade;   // ALTA / MEDIA / BAIXA

    @Column(columnDefinition = "TEXT")
    private String trechoOrigem;

    private Integer confianca;   // 0-100

}
