package com.fleetops.asignaciones.infrastructure.messaging.config;

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration
public class SqsConfig {

    // La cola queue_assignations es propiedad de Incidentes (productor).
    // Este servicio solo consume, por lo que no debe intentar crearla ni resolverla por nombre:
    // el rol IAM de este servicio solo tiene sqs:ReceiveMessage/DeleteMessage/ChangeMessageVisibility/
    // GetQueueAttributes (no sqs:GetQueueUrl ni sqs:CreateQueue). Por eso SQS_QUEUE_INCIDENTES_FALLA
    // debe ser la URL completa de la cola (https://sqs.<region>.amazonaws.com/<accountId>/<queueName>):
    // la librería detecta que ya es una URL y omite el GetQueueUrl, usándola directamente.
    //
    // No se fuerza ninguna verificación de existencia de la cola al arrancar (nada de
    // queueAttributeNames / GetQueueAttributes eager). spring-cloud-aws-sqs 3.1.1 solo ofrece
    // FAIL o CREATE como QueueNotFoundStrategy —no existe IGNORE, sigue siendo un PR sin mergear:
    // https://github.com/awspring/spring-cloud-aws/pull/1640— y CREATE no es viable porque el rol
    // IAM no tiene sqs:CreateQueue. Cualquier llamada eager a SQS durante el arranque del listener
    // es una llamada síncrona dentro de ApplicationContext.refresh(): si SQS no responde (cola
    // inexistente, IAM, red), lanza una excepción que tumba TODO el contexto, incluyendo REST/DB/Kafka.
    // Sin esa llamada eager, el listener solo falla en su bucle de polling en background (ya arrancada
    // la app), donde Spring Cloud AWS reintenta con backoff sin afectar al resto del servicio.
    @Bean
    public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(SqsAsyncClient sqsAsyncClient) {
        return SqsMessageListenerContainerFactory.builder()
                .sqsAsyncClient(sqsAsyncClient)
                .build();
    }
}
