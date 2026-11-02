package com.grupo3.nexo.tutores;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReporteRepository extends JpaRepository<Reporte, UUID> {

    boolean existsByReportanteIdAndTutorIdAndAbiertoTrue(UUID reportanteId, UUID tutorId);
}
