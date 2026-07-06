package com.fleetops.asignaciones.infrastructure.messaging.consumer;

import com.fleetops.asignaciones.application.port.in.ProcesarFallaMecanicaUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SqsIncidentesConsumer")
class SqsIncidentesConsumerTest {

    @Mock
    private ProcesarFallaMecanicaUseCase procesarFallaMecanicaUseCase;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private Acknowledgement acknowledgement;

    private SqsIncidentesConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new SqsIncidentesConsumer(procesarFallaMecanicaUseCase, objectMapper);
    }

    private String fallaMecanicaJson() {
        return "{" +
                "\"incident_id\":\"" + UUID.randomUUID() + "\"," +
                "\"vehicle_id\":\"" + UUID.randomUUID() + "\"," +
                "\"description\":\"Falla en el motor\"," +
                "\"driver_id\":\"" + UUID.randomUUID() + "\"," +
                "\"incident_type\":\"MECANICO\"," +
                "\"severity\":\"GRAVE\"," +
                "\"event_date\":\"2026-07-02T10:15:00Z\"}";
    }

    private String snsEnvelope(String type, String innerMessage) {
        return "{" +
                "\"Type\":\"" + type + "\"," +
                "\"MessageId\":\"" + UUID.randomUUID() + "\"," +
                "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:fleetops-incidentes-falla-mecanica\"," +
                "\"Message\":" + objectMapperWriteAsString(innerMessage) + "," +
                "\"Timestamp\":\"2026-07-02T10:15:01.000Z\"}";
    }

    private String objectMapperWriteAsString(String raw) {
        try {
            return objectMapper.writeValueAsString(raw);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("onFallaMecanica: deserializa el sobre SNS, procesa el payload real y hace ACK")
    void onFallaMecanica_procesaCorrectamente_yHaceAck() {
        String mensajeSqsBody = snsEnvelope("Notification", fallaMecanicaJson());

        consumer.onFallaMecanica(mensajeSqsBody, acknowledgement);

        verify(procesarFallaMecanicaUseCase).procesar(any());
        verify(acknowledgement).acknowledge();
    }

    @Test
    @DisplayName("onFallaMecanica: si el sobre SNS es JSON inválido, NO procesa ni hace ACK")
    void onFallaMecanica_siSobreInvalido_noProcesaNiHaceAck() {
        String mensajeSqsBody = "{invalid-json";

        consumer.onFallaMecanica(mensajeSqsBody, acknowledgement);

        verify(procesarFallaMecanicaUseCase, never()).procesar(any());
        verify(acknowledgement, never()).acknowledge();
    }

    @Test
    @DisplayName("onFallaMecanica: si el payload interno (Message) es JSON inválido, NO procesa ni hace ACK")
    void onFallaMecanica_siPayloadInternoInvalido_noProcesaNiHaceAck() {
        String mensajeSqsBody = snsEnvelope("Notification", "{invalid-json");

        consumer.onFallaMecanica(mensajeSqsBody, acknowledgement);

        verify(procesarFallaMecanicaUseCase, never()).procesar(any());
        verify(acknowledgement, never()).acknowledge();
    }

    @Test
    @DisplayName("onFallaMecanica: si el tipo de notificación SNS no es 'Notification', descarta el mensaje con ACK")
    void onFallaMecanica_siTipoNoEsNotification_descartaConAck() {
        String mensajeSqsBody = "{" +
                "\"Type\":\"SubscriptionConfirmation\"," +
                "\"MessageId\":\"" + UUID.randomUUID() + "\"," +
                "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:fleetops-incidentes-falla-mecanica\"," +
                "\"Token\":\"abc123\"}";

        consumer.onFallaMecanica(mensajeSqsBody, acknowledgement);

        verify(procesarFallaMecanicaUseCase, never()).procesar(any());
        verify(acknowledgement).acknowledge();
    }
}