package br.com.totvs.hermes.chat;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Rate limit por IP em memória (janela deslizante de 1 minuto).
 * <p>
 * Suficiente para uma instância só (Render free). Com várias instâncias, cada uma
 * conta separado — aí o certo é mover para Redis ou para o gateway.
 */
public class LimiteRequisicoes {

    private static final long JANELA_MS = 60_000;
    private static final int LIMPAR_A_CADA = 500;

    private final int limitePorMinuto;
    private final LongSupplier relogio;
    private final Map<String, Deque<Long>> acessos = new ConcurrentHashMap<>();
    private int chamadasDesdeLimpeza = 0;

    public LimiteRequisicoes(int limitePorMinuto) {
        this(limitePorMinuto, System::currentTimeMillis);
    }

    /** Construtor com relógio injetável, para teste. */
    LimiteRequisicoes(int limitePorMinuto, LongSupplier relogio) {
        this.limitePorMinuto = limitePorMinuto;
        this.relogio = relogio;
    }

    /**
     * Registra uma tentativa do IP.
     *
     * @return {@code true} se ainda está dentro do limite
     */
    public boolean permitir(String ip) {
        long agora = relogio.getAsLong();
        limparDeVezEmQuando(agora);

        Deque<Long> fila = acessos.computeIfAbsent(ip == null ? "?" : ip, k -> new ArrayDeque<>());
        synchronized (fila) {
            while (!fila.isEmpty() && agora - fila.peekFirst() >= JANELA_MS) fila.pollFirst();
            if (fila.size() >= limitePorMinuto) return false;
            fila.addLast(agora);
            return true;
        }
    }

    /** Remove IPs sem acesso recente para o mapa não crescer para sempre. */
    private synchronized void limparDeVezEmQuando(long agora) {
        if (++chamadasDesdeLimpeza < LIMPAR_A_CADA) return;
        chamadasDesdeLimpeza = 0;
        acessos.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                Long ultimo = e.getValue().peekLast();
                return ultimo == null || agora - ultimo >= JANELA_MS;
            }
        });
    }
}
