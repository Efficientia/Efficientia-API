package com.example.efficientia.feedbackrota.api;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class FeedbackRotaRequestTest {

    @Test
    void deveCriarRecordComSucesso() {
        UUID rotaId = UUID.randomUUID();
        String avaliacao = "OTIMA";
        List<String> motivos = List.of("TUDO");
        String comentario = "Perfeito";

        FeedbackRotaRequest request = new FeedbackRotaRequest(rotaId, avaliacao, motivos, comentario);

        assertThat(request.rotaId()).isEqualTo(rotaId);
        assertThat(request.avaliacao()).isEqualTo(avaliacao);
        assertThat(request.motivos()).isEqualTo(motivos);
        assertThat(request.comentario()).isEqualTo(comentario);
    }
}