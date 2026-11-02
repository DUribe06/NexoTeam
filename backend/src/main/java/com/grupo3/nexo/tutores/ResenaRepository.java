package com.grupo3.nexo.tutores;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResenaRepository extends JpaRepository<Resena, UUID> {

    @Query("""
        select new com.grupo3.nexo.tutores.TutorDtos$ResumenResenas(r.tutorId, avg(r.estrellas), count(r))
        from Resena r where r.tutorId in :tutorIds group by r.tutorId
        """)
    List<TutorDtos.ResumenResenas> resumenes(@Param("tutorIds") Collection<UUID> tutorIds);

    @Query(value = """
        select new com.grupo3.nexo.tutores.TutorDtos$ResenaDto(r.id, r.tutorId, a.nombre, a.handle, r.texto, r.estrellas, r.fecha)
        from Resena r join com.grupo3.nexo.usuarios.Usuario a on a.id = r.autorId
        where r.tutorId = :tutorId
        order by r.fecha desc, r.creadoEn desc, r.id
        """, countQuery = "select count(r) from Resena r where r.tutorId = :tutorId")
    Page<TutorDtos.ResenaDto> deTutor(@Param("tutorId") UUID tutorId, Pageable pagina);

    boolean existsByTutorIdAndAutorIdAndTexto(UUID tutorId, UUID autorId, String texto);
}
