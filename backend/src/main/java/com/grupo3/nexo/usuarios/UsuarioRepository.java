package com.grupo3.nexo.usuarios;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    boolean existsByCorreo(String correo);

    boolean existsByHandle(String handle);

    boolean existsByIdAndRol(UUID id, Rol rol);

    Optional<Usuario> findByCorreo(String correo);
}
