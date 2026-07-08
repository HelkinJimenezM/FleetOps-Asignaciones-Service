package com.fleetops.asignaciones.infrastructure.messaging.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.UUID;

/**
 * Mensaje que llega desde el microservicio de Incidentes.
 * Via topic: fleetops.incidentes.falla.mecanica
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FallaMecanicaMessage(
        @JsonProperty("incident_id")
        @JsonAlias({"incidentId"})
        String incidentId,

        @JsonProperty("vehicle_id")
        @JsonAlias({"vehicleId"})
        UUID vehicleId,

        @JsonProperty("description")
        String description,

        @JsonProperty("driver_id")
        @JsonAlias({"driverId"})
        UUID driverId,

        @JsonProperty("incident_type")
        @JsonAlias({"incidentType"})
        String incidentType,

        @JsonProperty("severity")
        @JsonAlias({"severity", "severity"})
        String severity,

        @JsonProperty("event_date")
        @JsonAlias({"event_date", "eventDate"})
        Date eventDate
) {}
