package com.grupo3.nexo.usuarios;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    private UUID id;

    private String nombre;

    /** Siempre normalizado: minúsculas y sin espacios (lo exige también la base). */
    private String correo;

    @Column(name = "contrasena_hash")
    private String contrasenaHash;

    @Enumerated(EnumType.STRING)
    private Rol rol;

    @Column(name = "documento_identidad")
    private String documentoIdentidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_cuenta")
    private EstadoCuenta estadoCuenta;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_verificacion_tutor")
    private EstadoVerificacionTutor estadoVerificacionTutor;

    @Column(name = "intentos_fallidos")
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    @Column(name = "terminos_version")
    private String terminosVersion;

    @Column(name = "terminos_aceptados_en")
    private Instant terminosAceptadosEn;

    @ElementCollection
    @CollectionTable(name = "tutor_materias", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "materia")
    private Set<String> materias = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "tutor_modalidades", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad")
    private Set<Modalidad> modalidades = new HashSet<>();

    protected Usuario() {
    }

    /** Cuenta recién registrada: sin verificar el correo y, si es tutor, pendiente de verificación académica. */
    public static Usuario nuevo(String nombre, String correo, String contrasenaHash, Rol rol,
                                String documentoIdentidad, String terminosVersion, Instant ahora) {
        Usuario u = new Usuario();
        u.id = UUID.randomUUID();
        u.nombre = nombre;
        u.correo = correo;
        u.contrasenaHash = contrasenaHash;
        u.rol = rol;
        u.documentoIdentidad = documentoIdentidad;
        u.estadoCuenta = EstadoCuenta.PENDIENTE_VERIFICACION_CORREO;
        u.estadoVerificacionTutor = rol == Rol.TUTOR ? EstadoVerificacionTutor.PENDIENTE : null;
        u.terminosVersion = terminosVersion;
        u.terminosAceptadosEn = ahora;
        return u;
    }

    public boolean estaBloqueado(Instant ahora) {
        return bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta);
    }

    /** Suma un intento fallido; al llegar al máximo bloquea la cuenta y reinicia el contador. Devuelve si quedó bloqueada. */
    public boolean registrarLoginFallido(Instant ahora, int maximoIntentos, Duration bloqueo) {
        intentosFallidos++;
        if (intentosFallidos >= maximoIntentos) {
            bloqueadoHasta = ahora.plus(bloqueo);
            intentosFallidos = 0;
            return true;
        }
        return false;
    }

    public void registrarLoginExitoso() {
        intentosFallidos = 0;
        bloqueadoHasta = null;
    }

    /** El dueño del correo demostró serlo con el código de 6 dígitos. */
    public void marcarCorreoVerificado() {
        if (estadoCuenta == EstadoCuenta.PENDIENTE_VERIFICACION_CORREO) {
            estadoCuenta = EstadoCuenta.ACTIVA;
        }
    }

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getContrasenaHash() { return contrasenaHash; }
    public Rol getRol() { return rol; }
    public String getDocumentoIdentidad() { return documentoIdentidad; }
    public EstadoCuenta getEstadoCuenta() { return estadoCuenta; }
    public EstadoVerificacionTutor getEstadoVerificacionTutor() { return estadoVerificacionTutor; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public Instant getBloqueadoHasta() { return bloqueadoHasta; }
    public String getTerminosVersion() { return terminosVersion; }
    public Instant getTerminosAceptadosEn() { return terminosAceptadosEn; }
    public Set<String> getMaterias() { return materias; }
    public Set<Modalidad> getModalidades() { return modalidades; }
}
