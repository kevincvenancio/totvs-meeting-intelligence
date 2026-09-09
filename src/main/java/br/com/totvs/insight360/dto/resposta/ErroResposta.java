package br.com.totvs.insight360.dto.resposta;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Corpo padrão de erro da API. Todo erro devolvido pelos endpoints usa este
 * formato, o que simplifica o tratamento no Front-End.
 *
 * @param instante momento em que o erro ocorreu
 * @param status   código HTTP
 * @param erro     descrição curta do status (ex.: "Not Found")
 * @param mensagem mensagem legível para o usuário final
 * @param caminho  URI que originou o erro
 * @param campos   erros de validação por campo (ausente quando não se aplica)
 */
public record ErroResposta(
        LocalDateTime instante,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<ErroCampo> campos
) {

    public static ErroResposta de(int status, String erro, String mensagem, String caminho) {
        return new ErroResposta(LocalDateTime.now(), status, erro, mensagem, caminho, null);
    }

    public static ErroResposta deValidacao(int status, String erro, String mensagem,
                                           String caminho, List<ErroCampo> campos) {
        return new ErroResposta(LocalDateTime.now(), status, erro, mensagem, caminho, campos);
    }
}
