package com.grupo3.nexo.usuarios;

import org.hibernate.annotations.DynamicUpdate;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

// Solo las columnas que cambiaron: cambiar el modo y editar el perfil a la vez no se pisan entre sí
@DynamicUpdate
@Entity
@Table(name = "usuarios")
public class Usuario {

    /** Quien usó la API en este plazo sale como conectado (`enLinea`). */
    public static final Duration VENTANA_EN_LINEA = Duration.ofMinutes(5);

    @Id
    private UUID id;

    private String nombre;

    /** Siempre normalizado: minúsculas y sin espacios (lo exige también la base). */
    private String correo;

    /** Null si la cuenta solo entra con Google. */
    @Column(name = "contrasena_hash")
    private String contrasenaHash;

    /** Id estable de la persona en Google; null si nunca ha entrado con Google. */
    @Column(name = "google_sub")
    private String googleSub;

    /** Nombre público, único; lo genera el servidor (`@valeriarios`). */
    private String handle;

    /** El modo en uso (Estudiante o Tutor, se cambia en el Perfil) o ADMIN, que no usa modos. */
    @Enumerated(EnumType.STRING)
    private Rol rol;

    @Column(name = "documento_identidad")
    private String documentoIdentidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_cuenta")
    private EstadoCuenta estadoCuenta;

    /** Se enciende la primera vez que la cuenta pasa a modo Tutor y no se vuelve a apagar. */
    @Column(name = "perfil_tutor_activo")
    private boolean perfilTutorActivo;

    private String universidad;

    private String carrera;

    private String bio;

    /** Tarifa por hora en pesos colombianos. */
    @Column(name = "precio_hora")
    private BigDecimal precioHora;

    @Column(name = "intentos_fallidos")
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    @Column(name = "terminos_version")
    private String terminosVersion;

    @Column(name = "terminos_aceptados_en")
    private Instant terminosAceptadosEn;

    /** Vacío hasta que el tutor crea su calendario semanal (RF7). */
    @Column(name = "calendario_creado_en")
    private Instant calendarioCreadoEn;

    /** Enlace a la foto de la cuenta de Google; se actualiza en cada inicio con Google. */
    @Column(name = "foto_google")
    private String fotoGoogle;

    /** La foto que subió en Nexo (en Cloudinary); gana sobre la de Google. */
    @Column(name = "foto_url")
    private String fotoPropia;

    /** Solo la escribe {@link UsuarioRepository#registrarActividad}: guardar la cuenta no debe pisarla con un valor viejo. */
    @Column(name = "ultima_actividad_en", insertable = false, updatable = false)
    private Instant ultimaActividadEn;

    @ElementCollection
    @CollectionTable(name = "tutor_materias", joinColumns = @JoinColumn(name = "usuario_id"))
    @OrderBy("posicion")
    private Set<MateriaTutor> materias = new LinkedHashSet<>();

    protected Usuario() {
    }

    /** Cuenta recién registrada: sin verificar el correo y en modo Estudiante, como toda cuenta. */
    public static Usuario nuevo(String nombre, String correo, String contrasenaHash, String handle,
                                String documentoIdentidad, String terminosVersion, Instant ahora) {
        Usuario u = new Usuario();
        u.id = UUID.randomUUID();
        u.nombre = nombre;
        u.correo = correo;
        u.contrasenaHash = contrasenaHash;
        u.handle = handle;
        u.rol = Rol.ESTUDIANTE;
        u.documentoIdentidad = documentoIdentidad;
        u.estadoCuenta = EstadoCuenta.PENDIENTE_VERIFICACION_CORREO;
        u.terminosVersion = terminosVersion;
        u.terminosAceptadosEn = ahora;
        return u;
    }

    /** Google ya confirmó el correo: no hace falta código ni contraseña. */
    public static Usuario nuevoConGoogle(String nombre, String correo, String googleSub, String handle,
                                         String terminosVersion, Instant ahora) {
        Usuario u = nuevo(nombre, correo, null, handle, null, terminosVersion, ahora);
        u.googleSub = googleSub;
        u.estadoCuenta = EstadoCuenta.ACTIVA;
        return u;
    }

    public void unirConGoogle(String googleSub) {
        this.googleSub = googleSub;
    }

    /**
     * Alguien registró este correo sin confirmarlo y ahora entra su dueño. Se borra esa contraseña: si la puso otra
     * persona, le daría acceso a la cuenta. Por lo mismo, las condiciones las acepta de nuevo quien sí es el dueño.
     */
    public void tomarCuentaSinConfirmar(String googleSub, String terminosVersion, Instant ahora) {
        this.googleSub = googleSub;
        contrasenaHash = null;
        this.terminosVersion = terminosVersion;
        terminosAceptadosEn = ahora;
        marcarCorreoVerificado();
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

    /** Cambia el modo en uso. La primera vez en modo Tutor deja creado el perfil de tutor, vacío. */
    public void cambiarModo(Modo modo) {
        if (modo == Modo.TUTOR) {
            perfilTutorActivo = true;
        }
        rol = modo.comoRol();
    }

    /** RF16: quien cambia la contraseña con el código demostró ser el dueño, así que también se levanta el bloqueo. */
    public void cambiarContrasena(String contrasenaHash) {
        this.contrasenaHash = contrasenaHash;
        registrarLoginExitoso();
    }

    public void crearCalendario(Instant ahora) {
        if (calendarioCreadoEn == null) {
            calendarioCreadoEn = ahora;
        }
    }

    public boolean tieneCalendario() { return calendarioCreadoEn != null; }

    /** Un tutor que los demás pueden ver: con perfil creado y la cuenta en uso. */
    public boolean esTutorPublico() {
        return perfilTutorActivo && estadoCuenta == EstadoCuenta.ACTIVA;
    }

    public boolean estaEnLinea(Instant ahora) {
        return ultimaActividadEn != null && !ultimaActividadEn.isBefore(ahora.minus(VENTANA_EN_LINEA));
    }

    public void cambiarBio(String bio) { this.bio = bio; }
    public void cambiarUniversidad(String universidad) { this.universidad = universidad; }
    public void cambiarCarrera(String carrera) { this.carrera = carrera; }
    public void cambiarPrecioHora(BigDecimal precioHora) { this.precioHora = precioHora; }

    /** Reemplaza la lista completa conservando su orden; sin espacios de sobra ni repetidas (sin distinguir mayúsculas). */
    public void reemplazarMaterias(List<String> nuevas) {
        Map<String, String> unicas = new LinkedHashMap<>();
        nuevas.forEach(m -> unicas.putIfAbsent(m.trim().toLowerCase(Locale.ROOT), m.trim()));
        materias.clear();
        int posicion = 0;
        for (String materia : unicas.values()) {
            materias.add(new MateriaTutor(materia, posicion++));
        }
    }

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getContrasenaHash() { return contrasenaHash; }
    public String getGoogleSub() { return googleSub; }
    public String getHandle() { return handle; }
    public Rol getRol() { return rol; }
    public String getDocumentoIdentidad() { return documentoIdentidad; }
    public EstadoCuenta getEstadoCuenta() { return estadoCuenta; }
    public boolean isPerfilTutorActivo() { return perfilTutorActivo; }
    public String getUniversidad() { return universidad; }
    public String getCarrera() { return carrera; }
    public String getBio() { return bio; }

    /** La foto que se muestra: la propia o, si no subió ninguna, la de Google. Vacía: la app muestra las iniciales. */
    public String getFotoUrl() { return fotoUrl(fotoPropia, fotoGoogle); }

    public static String fotoUrl(String fotoPropia, String fotoGoogle) {
        return fotoPropia != null ? fotoPropia : fotoGoogle;
    }

    public void cambiarFotoGoogle(String url) {
        if (url != null && url.startsWith("https://") && url.length() <= 1000) {
            this.fotoGoogle = url;
        }
    }

    public void cambiarFotoPropia(String url) { this.fotoPropia = url; }
    public BigDecimal getPrecioHora() { return precioHora; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public Instant getBloqueadoHasta() { return bloqueadoHasta; }
    public String getTerminosVersion() { return terminosVersion; }
    public Instant getTerminosAceptadosEn() { return terminosAceptadosEn; }

    /** Las materias en el orden en que el tutor las indicó. */
    public List<String> getMaterias() {
        return materias.stream().sorted(Comparator.comparingInt(MateriaTutor::getPosicion))
            .map(MateriaTutor::getMateria).toList();
    }
}
