package br.com.totvs.hermes.chat.llm;

/**
 * Uma mensagem da conversa no formato esperado pelas APIs compatíveis com OpenAI.
 *
 * @param papel    {@code system}, {@code user} ou {@code assistant}
 * @param conteudo texto da mensagem
 */
public record Mensagem(String papel, String conteudo) {

    public static Mensagem sistema(String conteudo) {
        return new Mensagem("system", conteudo);
    }

    public static Mensagem usuario(String conteudo) {
        return new Mensagem("user", conteudo);
    }

    public static Mensagem assistente(String conteudo) {
        return new Mensagem("assistant", conteudo);
    }
}
