package com.grupo3.nexo.usuarios;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** La foto que pone cada persona. La app la sube a Cloudinary y aquí solo se guarda la dirección. */
@Service
public class FotoPerfilService {

    private final UsuarioService usuarios;

    public FotoPerfilService(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @Transactional
    public PerfilPropioDto cambiar(UUID usuarioId, String url) {
        Usuario usuario = usuarios.cuentaEnSesion(usuarioId);
        usuario.cambiarFotoPropia(url);
        return PerfilPropioDto.de(usuario);
    }

    /** Sin la propia vuelve a verse la de Google, si la tiene. */
    @Transactional
    public PerfilPropioDto quitar(UUID usuarioId) {
        return cambiar(usuarioId, null);
    }
}
