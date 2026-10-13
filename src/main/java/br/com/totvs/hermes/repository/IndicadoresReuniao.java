package br.com.totvs.hermes.repository;

import br.com.totvs.hermes.model.RiscoChurn;
import br.com.totvs.hermes.model.Sentimento;

/**
 * Projeção leve de uma reunião analisada, só com as colunas que o dashboard agrega.
 * <p>
 * Evita trazer transcrições e demais CLOBs de todas as reuniões apenas para contar
 * e somar indicadores; {@code temOportunidade} já chega calculado pelo banco.
 */
public record IndicadoresReuniao(
        Long id,
        String cliente,
        RiscoChurn riscoChurn,
        Sentimento sentimento,
        Integer scoreQualidade,
        Integer scoreComercial,
        String produtosIdentificados,
        String concorrentesIdentificados,
        String categoriasPrincipais,
        Integer temOportunidade
) {}
