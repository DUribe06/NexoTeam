package com.grupo3.nexo.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.grupo3.nexo.admin.AdminDtos.ReporteAdmin;
import com.grupo3.nexo.admin.AdminDtos.RevisionAdmin;
import com.grupo3.nexo.admin.AdminDtos.UsuarioAdmin;

/** Lo que usa el panel web de /admin. `SeguridadConfig` exige el rol ADMIN en toda esta ruta. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/reportes")
    public List<ReporteAdmin> reportes() {
        return admin.reportes();
    }

    @PostMapping("/reportes/{id}/cerrar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cerrarReporte(@PathVariable String id) {
        admin.cerrarReporte(id);
    }

    @GetMapping("/revisiones")
    public List<RevisionAdmin> revisiones() {
        return admin.revisionesAbiertas();
    }

    @PostMapping("/revisiones/{id}/aceptar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void aceptarRevision(@PathVariable String id) {
        admin.resolverRevision(id, true);
    }

    @PostMapping("/revisiones/{id}/rechazar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rechazarRevision(@PathVariable String id) {
        admin.resolverRevision(id, false);
    }

    @GetMapping("/usuarios")
    public List<UsuarioAdmin> usuarios(@RequestParam(required = false) String q) {
        return admin.usuarios(q);
    }

    @PostMapping("/usuarios/{id}/suspender")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspender(@PathVariable String id) {
        admin.suspender(id);
    }

    @PostMapping("/usuarios/{id}/reactivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reactivar(@PathVariable String id) {
        admin.reactivar(id);
    }
}
