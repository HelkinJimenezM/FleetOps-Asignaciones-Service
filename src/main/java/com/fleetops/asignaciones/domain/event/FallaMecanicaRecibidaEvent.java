package com.fleetops.asignaciones.domain.event;

import java.util.Date;
import java.util.UUID;

public record FallaMecanicaRecibidaEvent(
        String incident_id,
        UUID vehicle_id,
        String description,
        UUID driver_id,
        String incident_type,
        String severity,
        Date event_date
) {}
