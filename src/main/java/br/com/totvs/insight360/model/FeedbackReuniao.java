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

    @Lob
    private String problemaIdentificado;

    private String categoriaProblema;

    @Lob
    private String motivoNaoIdentificadoAntes;

    @Lob
    private String sinaisNaConversa;

    @Lob
    private String comoIdentificarAntes;

    @Lob
    private String perguntasRecomendadas;

    @Lob
    private String acaoDeMelhoria;

    @Enumerated(EnumType.STRING)
    private NivelCriticidade nivelCriticidade;

    @Lob
    private String mensagemEducativa;
}
