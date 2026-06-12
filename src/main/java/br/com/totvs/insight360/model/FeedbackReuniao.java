package br.com.totvs.insight360.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Feedback educativo gerado para uma reunião.
 */
@Entity
@Table(name = "feedbacks_reuniao")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackReuniao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "reuniao_id")
    private Reuniao reuniao;

    @Column(columnDefinition = "TEXT")
    private String problemaIdentificado;

    private String categoriaProblema;

    @Column(columnDefinition = "TEXT")
    private String motivoNaoIdentificadoAntes;

    @Column(columnDefinition = "TEXT")
    private String sinaisNaConversa;

    @Column(columnDefinition = "TEXT")
    private String comoIdentificarAntes;

    @Column(columnDefinition = "TEXT")
    private String perguntasRecomendadas;

    @Column(columnDefinition = "TEXT")
    private String acaoDeMelhoria;

    @Enumerated(EnumType.STRING)
    private NivelCriticidade nivelCriticidade;

    @Column(columnDefinition = "TEXT")
    private String mensagemEducativa;
}
