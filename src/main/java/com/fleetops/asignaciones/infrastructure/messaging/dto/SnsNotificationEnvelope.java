package com.fleetops.asignaciones.infrastructure.messaging.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Sobre de notificación que SNS coloca como Body de cada mensaje SQS
 * cuando la cola está suscrita a un tópico SNS (fan-out) sin "raw message delivery".
 * El payload real del evento viaja serializado como string dentro de "Message".
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SnsNotificationEnvelope(
        @JsonProperty("Type")
        String type,

        @JsonProperty("MessageId")
        String messageId,

        @JsonProperty("TopicArn")
        String topicArn,

        @JsonProperty("Message")
        String message,

        @JsonProperty("Timestamp")
        String timestamp
) {}