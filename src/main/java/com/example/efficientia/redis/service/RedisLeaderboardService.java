package com.example.efficientia.redis.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

@Service
public class RedisLeaderboardService {

    private static final Logger log = LoggerFactory.getLogger(RedisLeaderboardService.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final String leaderboardKey;

    public RedisLeaderboardService(
            StringRedisTemplate stringRedisTemplate,
            @Value("${app.redis.leaderboard.key:efficientia:leaderboard:motoristas}") String leaderboardKey) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.leaderboardKey = leaderboardKey;
    }

    public record RankingItem(String motoristaId, double pontuacao, int posicao) {}

    /**
     * Incrementa ou decrementa a pontuação de um motorista no ranking.
     *
     * @param motoristaId identificador do motorista
     * @param pontos pontuação a ser somada (pode ser negativa)
     */
    public void incrementarPontuacaoMotorista(String motoristaId, double pontos) {
        if (motoristaId == null || motoristaId.isBlank()) {
            return;
        }
        try {
            stringRedisTemplate.opsForZSet().incrementScore(leaderboardKey, motoristaId, pontos);
            log.debug("Incremented score for driver {} by {} in key {}", motoristaId, pontos, leaderboardKey);
        } catch (Exception ex) {
            log.warn("Failed to increment score for driver {} in Redis leaderboard: {}", motoristaId, ex.getMessage());
        }
    }

    /**
     * Registra o impacto da conclusão de uma viagem no ranking do motorista.
     * Uma viagem concluída com sucesso confere pontos base (ex: 100), deduzindo penalidades
     * calculadas por mortalidade/anomalias durante o transporte.
     *
     * @param motoristaId identificador do motorista
     * @param penalidadeMortalidade penalidade acumulada por mortalidade de aves
     */
    public void registrarViagemConcluida(String motoristaId, double penalidadeMortalidade) {
        if (motoristaId == null || motoristaId.isBlank()) {
            return;
        }
        try {
            double pontosBase = 100.0;
            double pontuacaoFinal = Math.max(0.0, pontosBase - Math.max(0.0, penalidadeMortalidade));
            stringRedisTemplate.opsForZSet().incrementScore(leaderboardKey, motoristaId, pontuacaoFinal);
            log.debug("Trip recorded for driver {}. Added {} points (base={}, penalty={})",
                    motoristaId, pontuacaoFinal, pontosBase, penalidadeMortalidade);
        } catch (Exception ex) {
            log.warn("Failed to record completed trip for driver {} in Redis leaderboard: {}", motoristaId, ex.getMessage());
        }
    }

    /**
     * Retorna a lista dos motoristas no topo do ranking (maiores pontuações).
     *
     * @param limite quantidade máxima de motoristas a retornar
     * @return lista ordenada dos melhores motoristas ou lista vazia em caso de falha/indisponibilidade
     */
    public List<RankingItem> obterTopMotoristas(int limite) {
        if (limite <= 0) {
            return Collections.emptyList();
        }
        try {
            Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                    .reverseRangeWithScores(leaderboardKey, 0, limite - 1);

            if (tuples == null || tuples.isEmpty()) {
                return Collections.emptyList();
            }

            List<RankingItem> ranking = new ArrayList<>();
            int posicao = 1;
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                String motoristaId = tuple.getValue();
                Double score = tuple.getScore();
                ranking.add(new RankingItem(
                        motoristaId,
                        score != null ? score : 0.0,
                        posicao++
                ));
            }
            return ranking;
        } catch (Exception ex) {
            log.warn("Failed to fetch top drivers from Redis leaderboard: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }
}
