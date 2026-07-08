package com.fleetops.asignaciones.infrastructure.messaging.consumer;

import com.fleetops.asignaciones.application.port.in.ProcesarFallaMecanicaUseCase;
import com.fleetops.asignaciones.domain.event.FallaMecanicaRecibidaEvent;
import com.fleetops.asignaciones.infrastructure.messaging.dto.FallaMecanicaMessage;
import com.fleetops.asignaciones.infrastructure.messaging.dto.SnsNotificationEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.annotation.SqsListenerAcknowledgementMode;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SqsIncidentesConsumer {

    private final ProcesarFallaMecanicaUseCase procesarFallaMecanicaUseCase;
    private final ObjectMapper objectMapper;

    /**
     * Driving adapter: escucha fallas mecánicas reportadas por Incidentes vía SNS -> SQS.
     * El Body del mensaje SQS es un sobre de notificación SNS, no el payload directo:
     * hay que deserializarlo dos veces (sobre -> campo "Message" -> FallaMecanicaMessage).
     * ACK manual garantiza que el mensaje solo se borra de la cola si el procesamiento fue exitoso.
     */
    @SqsListener(
            queueNames = "${asignaciones.sqs.queues.queue_assignations}",
            acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL
    )
    public void onFallaMecanica(String mensajeSqsBody, Acknowledgement acknowledgement) {
        try {
            SnsNotificationEnvelope sobre = objectMapper.readValue(mensajeSqsBody, SnsNotificationEnvelope.class);

            if (!"Notification".equals(sobre.type()) || sobre.message() == null) {
                log.warn("Mensaje SQS ignorado: sobre SNS sin payload utilizable. type={} messageId={}",
                        sobre.type(), sobre.messageId());
                acknowledgement.acknowledge();
                return;
            }

            FallaMecanicaMessage mensaje = objectMapper.readValue(sobre.message(), FallaMecanicaMessage.class);

            log.info("Incidente recibido. incidente={} tipo={} severidad={} vehiculo={} conductor={}",
                    mensaje.incidentId(), mensaje.incidentType(), mensaje.severity(), mensaje.vehicleId(), mensaje.driverId());

            FallaMecanicaRecibidaEvent evento = new FallaMecanicaRecibidaEvent(
                    mensaje.incidentId(),
                    mensaje.vehicleId(),
                    mensaje.description(),
                    mensaje.driverId(),
                    mensaje.incidentType(),
                    mensaje.severity(),
                    mensaje.eventDate()
            );
            procesarFallaMecanicaUseCase.procesar(evento);
            acknowledgement.acknowledge();
        } catch (Exception ex) {
            log.error("Error procesando incidente recibido desde SQS: {}", ex.getMessage(), ex);
            // No se hace acknowledge(): el mensaje vuelve a quedar visible tras el visibility timeout
            // y se reintenta hasta agotar el maxReceiveCount configurado en la cola,
            // momento en que SQS lo mueve automáticamente a la Dead Letter Queue.
        }
    }
}