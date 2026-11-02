package com.grupo3.nexo.usuarios;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    boolean existsByCorreo(String correo);

    boolean existsByHandle(String handle);

    boolean existsByIdAndRol(UUID id, Rol rol);

    Optional<Usuario> findByCorreo(String correo);

    @EntityGraph(attributePaths = "materias")
    List<Usuario> findByIdIn(Collection<UUID> ids);

    /** Solo escribe si el último registro es anterior a `limite`, para no tocar la fila en cada petición. */
    @Modifying
    @Query(nativeQuery = true, value = """
        UPDATE usuarios SET ultima_actividad_en = :ahora
        WHERE id = :id AND (ultima_actividad_en IS NULL OR ultima_actividad_en < :limite)
        """)
    int registrarActividad(@Param("id") UUID id, @Param("ahora") Instant ahora, @Param("limite") Instant limite);
}
