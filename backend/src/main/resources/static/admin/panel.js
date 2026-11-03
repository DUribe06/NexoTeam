'use strict';

// El token solo vive en esta variable: recargar la página pide entrar de nuevo. Todo texto de usuarios entra con
// textContent, nunca como HTML: un motivo escrito por alguien no puede ejecutar nada aquí.
let token = null;
const $ = (id) => document.getElementById(id);

const FORMATO = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'America/Bogota' });
const fecha = (valor) => (valor ? FORMATO.format(new Date(valor)) : '');
const CIERRES = { TUTOR_NO_LLEGO: 'No envió el PIN a tiempo (tutor)', ESTUDIANTE_NO_LLEGO: 'No dio el PIN a tiempo (estudiante)' };
const ESTADOS = { ACTIVA: 'Activa', SUSPENDIDA: 'Suspendida', PENDIENTE_VERIFICACION_CORREO: 'Sin verificar el correo' };

function nodo(etiqueta, clase, texto) {
  const n = document.createElement(etiqueta);
  if (clase) n.className = clase;
  if (texto !== undefined) n.textContent = texto;
  return n;
}

async function api(ruta, metodo = 'GET', cuerpo) {
  const respuesta = await fetch(ruta, {
    method: metodo,
    headers: { ...(token ? { Authorization: 'Bearer ' + token } : {}), ...(cuerpo ? { 'Content-Type': 'application/json' } : {}) },
    body: cuerpo ? JSON.stringify(cuerpo) : undefined,
  });
  if ((respuesta.status === 401 || respuesta.status === 403) && token) {
    salir('Tu sesión venció. Entra de nuevo.');
    throw new Error('sesión vencida');
  }
  if (respuesta.status === 204) return null;
  const datos = await respuesta.json().catch(() => null);
  if (!respuesta.ok) throw new Error((datos && datos.mensaje) || 'Algo falló en el servidor.');
  return datos;
}

let temporizadorAviso;

function avisar(texto) {
  clearTimeout(temporizadorAviso);
  $('aviso').textContent = texto;
  $('aviso').hidden = false;
  temporizadorAviso = setTimeout(() => { $('aviso').hidden = true; }, 5000);
}

function boton(texto, accion, { clase, confirmar } = {}) {
  const b = nodo('button', clase, texto);
  b.addEventListener('click', async () => {
    if (confirmar && !window.confirm(confirmar)) return;
    b.disabled = true;
    try { await accion(); } catch (e) { avisar(e.message); b.disabled = false; }
  });
  return b;
}

function vacio(contenedor, texto) { contenedor.append(nodo('p', 'vacio', texto)); }

async function cargarRevisiones() {
  const lista = await api('/api/admin/revisiones');
  $('numRevisiones').textContent = lista.length || '';
  const seccion = $('revisiones');
  seccion.replaceChildren();
  if (!lista.length) vacio(seccion, 'No hay revisiones por resolver.');
  for (const r of lista) {
    const t = nodo('article', 'tarjeta');
    t.append(
      nodo('h2', '', r.persona),
      nodo('p', 'dato', r.correo),
      nodo('p', r.nivel === 'GRAVE' ? 'marca' : 'dato', 'Penalización ' + (r.nivel === 'GRAVE' ? 'grave' : 'normal') + ' · ' + (CIERRES[r.cierre] || '')),
      nodo('p', 'dato', r.materia + ' · sesión del ' + fecha(r.sesion) + ' · pedida el ' + fecha(r.pedidaEn)),
      nodo('p', 'motivo', r.motivo)
    );
    const acciones = nodo('div', 'acciones');
    acciones.append(
      boton('Retirar la penalización', async () => { await api(`/api/admin/revisiones/${r.id}/aceptar`, 'POST'); avisar('Penalización retirada.'); await cargarRevisiones(); }, { clase: 'primario', confirmar: 'Se retira la penalización y la pausa que causó. No se puede deshacer.' }),
      boton('Mantenerla', async () => { await api(`/api/admin/revisiones/${r.id}/rechazar`, 'POST'); avisar('La penalización se mantiene.'); await cargarRevisiones(); }, { confirmar: 'La penalización se mantiene. No se puede deshacer.' })
    );
    t.append(acciones);
    seccion.append(t);
  }
}

async function cargarReportes() {
  const lista = await api('/api/admin/reportes');
  $('numReportes').textContent = lista.filter((r) => !r.cerradoEn).length || '';
  const seccion = $('reportes');
  seccion.replaceChildren();
  if (!lista.length) vacio(seccion, 'No hay reportes.');
  for (const r of lista) {
    const t = nodo('article', 'tarjeta' + (r.cerradoEn ? ' cerrado' : ''));
    t.append(
      nodo('h2', '', 'Sobre ' + r.tutor),
      nodo('p', 'dato', r.correoTutor + ' · reportó ' + r.reportante + ' el ' + fecha(r.creadoEn)),
      nodo('p', 'motivo', r.motivo)
    );
    if (r.cerradoEn) {
      t.append(nodo('p', 'dato', 'Cerrado el ' + fecha(r.cerradoEn)));
    } else {
      const acciones = nodo('div', 'acciones');
      acciones.append(
        boton('Cerrar reporte', async () => { await api(`/api/admin/reportes/${r.id}/cerrar`, 'POST'); avisar('Reporte cerrado.'); await cargarReportes(); }),
        boton('Ver la cuenta', async () => { $('buscar').value = r.correoTutor; mostrar('usuarios'); await cargarUsuarios(); })
      );
      t.append(acciones);
    }
    seccion.append(t);
  }
}

async function cargarUsuarios() {
  const lista = await api('/api/admin/usuarios?q=' + encodeURIComponent($('buscar').value));
  const contenedor = $('listaUsuarios');
  contenedor.replaceChildren();
  if (!lista.length) vacio(contenedor, 'Ninguna cuenta coincide.');
  for (const u of lista) {
    const t = nodo('article', 'tarjeta');
    const suspendida = u.estadoCuenta === 'SUSPENDIDA';
    t.append(
      nodo('h2', '', u.nombre),
      nodo('p', 'dato', u.correo + ' · ' + u.handle + (u.esTutor ? ' · tutor' : '')),
      nodo('p', suspendida ? 'marca' : 'dato', 'Cuenta: ' + (ESTADOS[u.estadoCuenta] || u.estadoCuenta)),
      nodo('p', 'dato', `Penalizaciones en 90 días: ${u.penalizacionesComoTutor} como tutor, ${u.penalizacionesComoEstudiante} como estudiante · Reportes abiertos: ${u.reportesAbiertos}`)
    );
    if (u.pausaHasta) t.append(nodo('p', 'marca', 'En pausa hasta el ' + fecha(u.pausaHasta)));
    const acciones = nodo('div', 'acciones');
    if (suspendida) {
      acciones.append(boton('Reactivar', async () => { await api(`/api/admin/usuarios/${u.id}/reactivar`, 'POST'); avisar('Cuenta reactivada.'); await cargarUsuarios(); }));
    } else if (u.estadoCuenta === 'ACTIVA') {
      acciones.append(boton('Suspender', async () => { await api(`/api/admin/usuarios/${u.id}/suspender`, 'POST'); avisar('Cuenta suspendida.'); await cargarUsuarios(); }, { clase: 'peligro', confirmar: `Se suspende a ${u.nombre} y se cierran sus sesiones. Sus tutorías agendadas no se cancelan.` }));
    }
    t.append(acciones);
    contenedor.append(t);
  }
}

const CARGAS = { revisiones: cargarRevisiones, reportes: cargarReportes, usuarios: cargarUsuarios };

function mostrar(seccion) {
  for (const nombre of Object.keys(CARGAS)) $(nombre).hidden = nombre !== seccion;
  document.querySelectorAll('[role=tab]').forEach((t) => t.setAttribute('aria-selected', String(t.dataset.seccion === seccion)));
}

function salir(mensaje) {
  token = null;
  $('panel').hidden = true;
  $('acceso').hidden = false;
  $('contrasena').value = '';
  $('errorAcceso').textContent = mensaje || '';
  $('errorAcceso').hidden = !mensaje;
}

$('formAcceso').addEventListener('submit', async (evento) => {
  evento.preventDefault();
  $('errorAcceso').hidden = true;
  try {
    const sesion = await api('/api/auth/login', 'POST', { correo: $('correo').value, contrasena: $('contrasena').value });
    token = sesion.tokenAcceso;
    if (sesion.usuario.rol !== 'ADMIN') {
      salir('Esta cuenta no es de administración.');
      return;
    }
    $('acceso').hidden = true;
    $('panel').hidden = false;
    $('contrasena').value = '';
    mostrar('revisiones');
    await Promise.all([cargarRevisiones(), cargarReportes(), cargarUsuarios()]);
  } catch (e) {
    if (!token) { $('errorAcceso').textContent = e.message; $('errorAcceso').hidden = false; }
  }
});

document.querySelectorAll('[role=tab]').forEach((t) => t.addEventListener('click', async () => {
  mostrar(t.dataset.seccion);
  try { await CARGAS[t.dataset.seccion](); } catch (e) { avisar(e.message); }
}));

let espera;
$('buscar').addEventListener('input', () => {
  clearTimeout(espera);
  espera = setTimeout(() => cargarUsuarios().catch((e) => avisar(e.message)), 300);
});
$('salir').addEventListener('click', () => salir());
