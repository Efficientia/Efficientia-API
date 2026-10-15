package com.example.efficientia.redis.service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisLeaderboardServiceTest {

    private static final String LEADERBOARD_KEY = "efficientia:leaderboard:motoristas";

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private RedisLeaderboardService service;

    @BeforeEach
    void setUp() {
        service = new RedisLeaderboardService(stringRedisTemplate, LEADERBOARD_KEY);
    }

    // --- incrementarPontuacaoMotorista tests ---

    @Test
    @DisplayName("Should increment score when driverId is valid")
    void incrementarPontuacaoMotorista_ValidDriver_CallsIncrementScore() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);

        service.incrementarPontuacaoMotorista("driver-123", 25.5);

        verify(zSetOperations).incrementScore(LEADERBOARD_KEY, "driver-123", 25.5);
    }

    @Test
    @DisplayName("Should do nothing when driverId is null or blank")
    void incrementarPontuacaoMotorista_NullOrBlankDriver_DoesNothing() {
        service.incrementarPontuacaoMotorista(null, 10.0);
        service.incrementarPontuacaoMotorista("", 10.0);
        service.incrementarPontuacaoMotorista("   ", 10.0);

        verify(stringRedisTemplate, never()).opsForZSet();
    }

    @Test
    @DisplayName("Should catch exception and not propagate error when Redis fails")
    void incrementarPontuacaoMotorista_RedisThrowsException_CatchesSilently() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.incrementScore(anyString(), anyString(), anyDouble()))
                .thenThrow(new RedisConnectionFailureException("Connection refused"));

        // Should not throw
        service.incrementarPontuacaoMotorista("driver-123", 10.0);

        verify(zSetOperations).incrementScore(LEADERBOARD_KEY, "driver-123", 10.0);
    }

    // --- registrarViagemConcluida tests ---

    @Test
    @DisplayName("Should do nothing when driverId is null or blank in registrarViagemConcluida")
    void registrarViagemConcluida_NullOrBlankDriver_DoesNothing() {
        service.registrarViagemConcluida(null, 5.0);
        service.registrarViagemConcluida("", 5.0);
        service.registrarViagemConcluida("   ", 5.0);

        verify(stringRedisTemplate, never()).opsForZSet();
    }

    @Test
    @DisplayName("Should calculate score with no mortality (zero penalty): adds 100 points")
    void registrarViagemConcluida_ZeroMortality_AddsFullBasePoints() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);

        service.registrarViagemConcluida("driver-123", 0.0);

        verify(zSetOperations).incrementScore(LEADERBOARD_KEY, "driver-123", 100.0);
    }

    @Test
    @DisplayName("Should calculate score with negative mortality penalty: treated as 0 penalty, adds 100 points")
    void registrarViagemConcluida_NegativeMortality_AddsFullBasePoints() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);

        service.registrarViagemConcluida("driver-123", -15.0);

        verify(zSetOperations).incrementScore(LEADERBOARD_KEY, "driver-123", 100.0);
    }

    @Test
    @DisplayName("Should calculate score with positive mortality penalty: deducts penalty from base points")
    void registrarViagemConcluida_PositiveMortality_DeductsPenalty() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);

        service.registrarViagemConcluida("driver-123", 30.0);

        verify(zSetOperations).incrementScore(LEADERBOARD_KEY, "driver-123", 70.0);
    }

    @Test
    @DisplayName("Should clamp score to zero when mortality penalty exceeds base points")
    void registrarViagemConcluida_MortalityExceedsBase_ClampsToZero() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);

        service.registrarViagemConcluida("driver-123", 150.0);

        verify(zSetOperations).incrementScore(LEADERBOARD_KEY, "driver-123", 0.0);
    }

    @Test
    @DisplayName("Should catch exception and not propagate error when registrarViagemConcluida fails on Redis")
    void registrarViagemConcluida_RedisThrowsException_CatchesSilently() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.incrementScore(anyString(), anyString(), anyDouble()))
                .thenThrow(new RedisConnectionFailureException("Redis down"));

        // Should not throw
        service.registrarViagemConcluida("driver-123", 10.0);

        verify(zSetOperations).incrementScore(LEADERBOARD_KEY, "driver-123", 90.0);
    }

    // --- obterTopMotoristas tests ---

    @Test
    @DisplayName("Should return empty list immediately when limit is zero or negative")
    void obterTopMotoristas_NonPositiveLimit_ReturnsEmptyList() {
        List<RedisLeaderboardService.RankingItem> resultZero = service.obterTopMotoristas(0);
        List<RedisLeaderboardService.RankingItem> resultNeg = service.obterTopMotoristas(-5);

        assertThat(resultZero).isEmpty();
        assertThat(resultNeg).isEmpty();
        verify(stringRedisTemplate, never()).opsForZSet();
    }

    @Test
    @DisplayName("Should map tuples correctly to 1-based ranking list with scores")
    void obterTopMotoristas_ValidTuples_MapsToRankingItems() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);

        Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
        tuples.add(new DefaultTypedTuple<>("driver-alpha", 250.0));
        tuples.add(new DefaultTypedTuple<>("driver-beta", 180.5));
        tuples.add(new DefaultTypedTuple<>("driver-gamma", null)); // score is null branch

        when(zSetOperations.reverseRangeWithScores(LEADERBOARD_KEY, 0, 2)).thenReturn(tuples);

        List<RedisLeaderboardService.RankingItem> result = service.obterTopMotoristas(3);

        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo(new RedisLeaderboardService.RankingItem("driver-alpha", 250.0, 1));
        assertThat(result.get(1)).isEqualTo(new RedisLeaderboardService.RankingItem("driver-beta", 180.5, 2));
        assertThat(result.get(2)).isEqualTo(new RedisLeaderboardService.RankingItem("driver-gamma", 0.0, 3));
    }

    @Test
    @DisplayName("Should return empty list when reverseRangeWithScores returns null or empty set")
    void obterTopMotoristas_NullOrEmptyTuples_ReturnsEmptyList() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRangeWithScores(LEADERBOARD_KEY, 0, 4)).thenReturn(null);

        List<RedisLeaderboardService.RankingItem> resultNull = service.obterTopMotoristas(5);
        assertThat(resultNull).isEmpty();

        when(zSetOperations.reverseRangeWithScores(LEADERBOARD_KEY, 0, 4)).thenReturn(Collections.emptySet());

        List<RedisLeaderboardService.RankingItem> resultEmpty = service.obterTopMotoristas(5);
        assertThat(resultEmpty).isEmpty();
    }

    @Test
    @DisplayName("Should catch exception and return empty list when Redis throws error")
    void obterTopMotoristas_RedisThrowsException_ReturnsEmptyList() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRangeWithScores(anyString(), anyLong(), anyLong()))
                .thenThrow(new RedisConnectionFailureException("Timeout"));

        List<RedisLeaderboardService.RankingItem> result = service.obterTopMotoristas(5);

        assertThat(result).isEmpty();
    }

    // --- RankingItem record tests ---

    @Test
    @DisplayName("Should verify RankingItem record properties, equals, hashCode, and toString")
    void rankingItemRecord_BehavesAsExpected() {
        RedisLeaderboardService.RankingItem item1 = new RedisLeaderboardService.RankingItem("driver-1", 100.0, 1);
        RedisLeaderboardService.RankingItem item2 = new RedisLeaderboardService.RankingItem("driver-1", 100.0, 1);
        RedisLeaderboardService.RankingItem item3 = new RedisLeaderboardService.RankingItem("driver-2", 50.0, 2);

        assertThat(item1.motoristaId()).isEqualTo("driver-1");
        assertThat(item1.pontuacao()).isEqualTo(100.0);
        assertThat(item1.posicao()).isEqualTo(1);

        assertThat(item1).isEqualTo(item2);
        assertThat(item1.hashCode()).isEqualTo(item2.hashCode());
        assertThat(item1).isNotEqualTo(item3);
        assertThat(item1.toString()).contains("driver-1", "100.0", "1");
    }
}
