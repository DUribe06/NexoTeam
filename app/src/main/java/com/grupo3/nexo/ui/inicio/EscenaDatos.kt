package com.grupo3.nexo.ui.inicio

import com.grupo3.nexo.R

/*
 * Datos de las escenas de BarraSesion, generados a partir de las hojas de piezas (frontend/contexto/animacion inicio).
 * Unidades de escena: 270 de alto con el suelo abajo; x desde el borde izquierdo de la escena. id 0 = pieza sin imagen
 * (raíz invisible o elemento vectorial dibujado en código). padre = índice de la pieza de la que cuelga (-1: ninguna).
 */

internal class Capa(
    val n: String, val id: Int, val x: Float, val y: Float, val px: Float, val py: Float,
    val padre: Int, val w: Float = 0f, val h: Float = 0f
)

internal class Rig(val ancho: Float, val capas: List<Capa>)

internal val rigSola = Rig(166.9f, listOf(
    Capa("sombra", 0, 0.0f, 262.5f, 83.4f, 270.0f, -1, 166.9f, 14.9f),
    Capa("silla", R.drawable.rig_sola_silla, 9.9f, 137.9f, 24.8f, 268.8f, -1),
    Capa("mesa_atras", R.drawable.rig_sola_mesa_atras, 89.3f, 182.0f, 148.8f, 268.8f, -1),
    Capa("e_piernas", R.drawable.rig_sola_e_piernas, 26.7f, 174.2f, 55.8f, 268.8f, -1),
    Capa("e_cabeza", R.drawable.rig_sola_e_cabeza, 54.0f, 73.1f, 70.1f, 128.0f, 5),
    Capa("e_torso", R.drawable.rig_sola_e_torso, 21.1f, 120.6f, 49.6f, 191.3f, -1),
    Capa("mesa", R.drawable.rig_sola_mesa, 64.5f, 160.3f, 148.8f, 268.8f, -1),
    Capa("portatil", R.drawable.rig_sola_portatil, 80.0f, 138.6f, 117.8f, 172.7f, -1),
    Capa("e_ante2", R.drawable.rig_sola_e_ante2, 74.7f, 153.4f, 76.9f, 158.4f, 5),
    Capa("e_ante", R.drawable.rig_sola_e_ante, 55.2f, 159.3f, 52.7f, 169.6f, 5)
))

internal val rigEnviada = Rig(149.6f, listOf(
    Capa("e_piernaI", R.drawable.rig_enviada_e_piernai, 13.3f, 149.8f, 26.1f, 142.4f, 4),
    Capa("e_piernaD", R.drawable.rig_enviada_e_piernad, 38.5f, 149.8f, 50.8f, 142.4f, 4),
    Capa("e_pieI", R.drawable.rig_enviada_e_piei, 6.6f, 252.6f, 29.2f, 252.6f, 0),
    Capa("e_pieD", R.drawable.rig_enviada_e_pied, 45.2f, 251.4f, 51.6f, 251.4f, 1),
    Capa("e_pelvis", R.drawable.rig_enviada_e_pelvis, 14.1f, 140.3f, 38.7f, 154.0f, -1),
    Capa("e_cabeza", R.drawable.rig_enviada_e_cabeza, 19.1f, 15.6f, 38.7f, 79.0f, 6),
    Capa("e_torso", R.drawable.rig_enviada_e_torso, 5.4f, 74.8f, 38.9f, 148.6f, 4),
    Capa("e_brazoI", R.drawable.rig_enviada_e_brazoi, 1.7f, 125.0f, 9.7f, 130.8f, 9),
    Capa("e_manoI", R.drawable.rig_enviada_e_manoi, 0.0f, 152.3f, 5.6f, 155.2f, 7),
    Capa("e_mangaI", R.drawable.rig_enviada_e_mangai, 3.7f, 94.7f, 14.3f, 102.8f, 6),
    Capa("e_brazoD", R.drawable.rig_enviada_e_brazod, 61.7f, 125.8f, 67.3f, 131.6f, 12),
    Capa("e_manoD", R.drawable.rig_enviada_e_manod, 66.3f, 154.4f, 71.9f, 157.3f, 10),
    Capa("e_mangaD", R.drawable.rig_enviada_e_mangad, 54.3f, 94.3f, 62.7f, 102.7f, 6),
    Capa("burbuja", 0, 70.9f, 17.2f, 110.2f, 42.1f, -1, 78.7f, 49.7f),
    Capa("punto0", 0, 90.7f, 38.8f, 95.3f, 43.3f, 13, 9.1f, 9.1f),
    Capa("punto1", 0, 110.2f, 38.8f, 114.8f, 43.3f, 13, 9.1f, 9.1f),
    Capa("punto2", 0, 129.7f, 38.8f, 134.3f, 43.3f, 13, 9.1f, 9.1f)
))

internal val rigAgendada = Rig(200.6f, listOf(
    Capa("calendario", 0, 101.2f, 50.8f, 150.9f, 95.1f, -1, 99.4f, 88.7f),
    Capa("e_piernaI", R.drawable.rig_agendada_e_piernai, 13.3f, 149.8f, 26.1f, 142.4f, 5),
    Capa("e_piernaD", R.drawable.rig_agendada_e_piernad, 38.5f, 149.8f, 50.8f, 142.4f, 5),
    Capa("e_pieI", R.drawable.rig_agendada_e_piei, 6.6f, 252.6f, 29.2f, 252.6f, 1),
    Capa("e_pieD", R.drawable.rig_agendada_e_pied, 45.2f, 251.4f, 51.6f, 251.4f, 2),
    Capa("e_pelvis", R.drawable.rig_agendada_e_pelvis, 14.1f, 140.3f, 38.7f, 154.0f, -1),
    Capa("e_cabeza", R.drawable.rig_agendada_e_cabeza, 19.1f, 15.6f, 38.7f, 79.0f, 7),
    Capa("e_torso", R.drawable.rig_agendada_e_torso, 5.4f, 74.8f, 38.9f, 148.6f, 5),
    Capa("e_brazoI", R.drawable.rig_agendada_e_brazoi, 1.7f, 125.0f, 9.7f, 130.8f, 10),
    Capa("e_manoI", R.drawable.rig_agendada_e_manoi, 0.0f, 152.3f, 5.6f, 155.2f, 8),
    Capa("e_mangaI", R.drawable.rig_agendada_e_mangai, 3.7f, 94.7f, 14.3f, 102.8f, 7),
    Capa("e_brazoD", R.drawable.rig_agendada_e_brazod, 61.7f, 125.8f, 67.3f, 131.6f, 14),
    Capa("e_manoD", R.drawable.rig_agendada_e_manod, 66.3f, 154.4f, 71.9f, 157.3f, 11),
    Capa("e_senala", R.drawable.rig_agendada_e_senala, 63.4f, 155.2f, 71.9f, 157.3f, 11),
    Capa("e_mangaD", R.drawable.rig_agendada_e_mangad, 54.3f, 94.3f, 62.7f, 102.7f, 7),
    Capa("marca", 0, 143.0f, 82.8f, 150.9f, 90.7f, -1, 15.7f, 15.7f)
))

internal val rigHoy = Rig(87.0f, listOf(
    Capa("e_piernaI", R.drawable.rig_hoy_e_piernai, 23.2f, 149.8f, 36.0f, 142.4f, 4),
    Capa("e_piernaD", R.drawable.rig_hoy_e_piernad, 48.5f, 149.8f, 60.7f, 142.4f, 4),
    Capa("e_pieI", R.drawable.rig_hoy_e_piei, 16.6f, 252.6f, 39.2f, 252.6f, 0),
    Capa("e_pieD", R.drawable.rig_hoy_e_pied, 55.1f, 251.4f, 61.5f, 251.4f, 1),
    Capa("e_pelvis", R.drawable.rig_hoy_e_pelvis, 24.0f, 140.3f, 48.7f, 154.0f, -1),
    Capa("e_cabeza", R.drawable.rig_hoy_e_cabeza, 29.0f, 15.6f, 48.7f, 79.0f, 6),
    Capa("e_torso", R.drawable.rig_hoy_e_torso, 15.3f, 74.8f, 48.9f, 148.6f, 4),
    Capa("e_brazoI", R.drawable.rig_hoy_e_brazoi, 11.6f, 125.0f, 19.7f, 130.8f, 10),
    Capa("e_manoI", R.drawable.rig_hoy_e_manoi, 9.9f, 152.3f, 15.5f, 155.2f, 7),
    Capa("e_reloj", R.drawable.rig_hoy_e_reloj, 0.0f, 152.7f, 15.5f, 155.2f, 7),
    Capa("e_mangaI", R.drawable.rig_hoy_e_mangai, 13.7f, 94.7f, 24.3f, 102.8f, 6),
    Capa("e_brazoD", R.drawable.rig_hoy_e_brazod, 71.7f, 125.8f, 77.3f, 131.6f, 13),
    Capa("e_manoD", R.drawable.rig_hoy_e_manod, 76.2f, 154.4f, 81.8f, 157.3f, 11),
    Capa("e_mangaD", R.drawable.rig_hoy_e_mangad, 64.2f, 94.3f, 72.7f, 102.7f, 6)
))

internal val rigYaCasi = Rig(469.4f, listOf(
    Capa("e_raiz", 0, 0.0f, -144.4f, 302.1f, 154.4f, -1, 0.0f, 0.0f),
    Capa("e_pierna2", R.drawable.rig_ya_casi_e_pierna2, 334.5f, 142.7f, 344.4f, 152.4f, 2),
    Capa("e_muslo2", R.drawable.rig_ya_casi_e_muslo2, 284.1f, 139.4f, 298.2f, 152.8f, 0),
    Capa("e_ante2", R.drawable.rig_ya_casi_e_ante2, 297.8f, 113.0f, 304.3f, 119.1f, 4),
    Capa("e_brazo2", R.drawable.rig_ya_casi_e_brazo2, 289.9f, 82.0f, 298.2f, 89.7f, 7),
    Capa("e_pierna", R.drawable.rig_ya_casi_e_pierna, 338.4f, 144.2f, 348.3f, 154.0f, 6),
    Capa("e_muslo", R.drawable.rig_ya_casi_e_muslo, 288.0f, 141.0f, 302.1f, 154.4f, 0),
    Capa("e_torso", R.drawable.rig_ya_casi_e_torso, 280.0f, 73.5f, 302.1f, 154.4f, 0),
    Capa("e_cabeza", R.drawable.rig_ya_casi_e_cabeza, 282.7f, 31.0f, 304.0f, 76.5f, 7),
    Capa("e_ante", R.drawable.rig_ya_casi_e_ante, 301.7f, 114.6f, 308.2f, 120.6f, 10),
    Capa("e_brazo", R.drawable.rig_ya_casi_e_brazo, 293.8f, 83.6f, 302.1f, 91.3f, 7),
    Capa("t_raiz", 0, 0.0f, -144.4f, 443.8f, 156.1f, -1, 0.0f, 0.0f),
    Capa("t_pierna2", R.drawable.rig_ya_casi_t_pierna2, 381.5f, 143.7f, 405.5f, 155.6f, 13),
    Capa("t_muslo2", R.drawable.rig_ya_casi_t_muslo2, 393.4f, 139.9f, 448.0f, 154.4f, 11),
    Capa("t_ante2", R.drawable.rig_ya_casi_t_ante2, 376.5f, 117.6f, 439.8f, 124.7f, 15),
    Capa("t_brazo2", R.drawable.rig_ya_casi_t_brazo2, 431.7f, 84.3f, 446.3f, 92.4f, 18),
    Capa("t_pierna", R.drawable.rig_ya_casi_t_pierna, 377.3f, 145.4f, 401.4f, 157.3f, 17),
    Capa("t_muslo", R.drawable.rig_ya_casi_t_muslo, 389.2f, 141.6f, 443.8f, 156.1f, 11),
    Capa("t_torso", R.drawable.rig_ya_casi_t_torso, 395.6f, 74.4f, 443.8f, 156.1f, 11),
    Capa("t_cabeza", R.drawable.rig_ya_casi_t_cabeza, 425.4f, 37.6f, 436.2f, 79.0f, 18),
    Capa("t_ante", R.drawable.rig_ya_casi_t_ante, 372.3f, 119.3f, 435.6f, 126.4f, 21),
    Capa("t_brazo", R.drawable.rig_ya_casi_t_brazo, 427.5f, 86.0f, 442.1f, 94.1f, 18)
))

internal val rigSesion = Rig(290.0f, listOf(
    Capa("sombra", 0, 0.0f, 262.5f, 145.0f, 270.0f, -1, 290.0f, 14.9f),
    Capa("silla", R.drawable.rig_sesion_silla, 9.9f, 137.9f, 24.8f, 268.8f, -1),
    Capa("silla_t", R.drawable.rig_sesion_silla_t, 224.2f, 137.9f, 266.6f, 268.8f, -1),
    Capa("mesa_atras", R.drawable.rig_sesion_mesa_atras, 89.3f, 182.0f, 148.8f, 268.8f, -1),
    Capa("e_piernas", R.drawable.rig_sesion_e_piernas, 26.7f, 174.2f, 55.8f, 268.8f, -1),
    Capa("t_piernas", R.drawable.rig_sesion_t_piernas, 175.8f, 173.3f, 226.3f, 268.8f, -1),
    Capa("e_cabeza", R.drawable.rig_sesion_e_cabeza, 54.0f, 73.1f, 70.1f, 128.0f, 7),
    Capa("e_torso", R.drawable.rig_sesion_e_torso, 21.1f, 120.6f, 49.6f, 191.3f, -1),
    Capa("t_cabeza", R.drawable.rig_sesion_t_cabeza, 207.7f, 74.5f, 228.8f, 123.7f, 9),
    Capa("t_torso", R.drawable.rig_sesion_t_torso, 208.0f, 114.1f, 238.7f, 191.3f, -1),
    Capa("mesa", R.drawable.rig_sesion_mesa, 64.5f, 160.3f, 148.8f, 268.8f, -1),
    Capa("portatil", R.drawable.rig_sesion_portatil, 80.0f, 138.6f, 117.8f, 172.7f, -1),
    Capa("libro", R.drawable.rig_sesion_libro, 164.3f, 160.3f, 192.2f, 169.6f, -1),
    Capa("e_ante2", R.drawable.rig_sesion_e_ante2, 74.7f, 153.4f, 76.9f, 158.4f, 7),
    Capa("e_ante", R.drawable.rig_sesion_e_ante, 55.2f, 159.3f, 52.7f, 169.6f, 7),
    Capa("t_ante2", R.drawable.rig_sesion_t_ante2, 170.8f, 145.7f, 216.4f, 152.8f, 9),
    Capa("t_mano", R.drawable.rig_sesion_t_mano, 199.4f, 157.2f, 212.7f, 163.4f, 20),
    Capa("t_senala", R.drawable.rig_sesion_t_senala, 192.2f, 160.9f, 212.7f, 163.0f, 20),
    Capa("t_abierta", R.drawable.rig_sesion_t_abierta, 192.8f, 151.3f, 212.7f, 163.0f, 20),
    Capa("t_pagina", R.drawable.rig_sesion_t_pagina, 194.1f, 160.6f, 212.7f, 163.0f, 20),
    Capa("t_ante", R.drawable.rig_sesion_t_ante, 209.3f, 151.3f, 253.0f, 164.0f, 9)
))

internal val rigConectando = Rig(302.5f, listOf(
    Capa("sombra", 0, 0.0f, 262.5f, 83.4f, 270.0f, -1, 166.9f, 14.9f),
    Capa("silla", R.drawable.rig_conectando_silla, 9.9f, 137.9f, 24.8f, 268.8f, -1),
    Capa("mesa_atras", R.drawable.rig_conectando_mesa_atras, 89.3f, 182.0f, 148.8f, 268.8f, -1),
    Capa("e_piernas", R.drawable.rig_conectando_e_piernas, 26.7f, 174.2f, 55.8f, 268.8f, -1),
    Capa("e_cabeza", R.drawable.rig_conectando_e_cabeza, 54.0f, 73.1f, 70.1f, 128.0f, 5),
    Capa("e_torso", R.drawable.rig_conectando_e_torso, 21.1f, 120.6f, 49.6f, 191.3f, -1),
    Capa("mesa", R.drawable.rig_conectando_mesa, 64.5f, 160.3f, 148.8f, 268.8f, -1),
    Capa("portatil", R.drawable.rig_conectando_portatil, 80.0f, 138.6f, 117.8f, 172.7f, -1),
    Capa("e_ante2", R.drawable.rig_conectando_e_ante2, 74.7f, 153.4f, 76.9f, 158.4f, 5),
    Capa("e_ante", R.drawable.rig_conectando_e_ante, 55.2f, 159.3f, 52.7f, 169.6f, 5),
    Capa("tarjeta", 0, 165.8f, 54.6f, 234.1f, 103.3f, -1, 136.7f, 97.4f),
    Capa("t_cabeza", R.drawable.rig_conectando_t_cabeza, 212.3f, 50.5f, 234.4f, 113.2f, 12),
    Capa("t_busto", R.drawable.rig_conectando_t_busto, 188.3f, 109.2f, 234.1f, 152.0f, -1),
    Capa("enlinea", 0, 172.4f, 61.2f, 176.1f, 65.0f, -1, 7.5f, 7.5f),
    Capa("conecta0", 0, 210.9f, 98.7f, 215.5f, 103.3f, -1, 9.1f, 9.1f),
    Capa("conecta1", 0, 229.2f, 98.7f, 233.7f, 103.3f, -1, 9.1f, 9.1f),
    Capa("conecta2", 0, 247.4f, 98.7f, 252.0f, 103.3f, -1, 9.1f, 9.1f)
))

internal val rigLlamada = Rig(302.5f, listOf(
    Capa("sombra", 0, 0.0f, 262.5f, 83.4f, 270.0f, -1, 166.9f, 14.9f),
    Capa("silla", R.drawable.rig_conectando_silla, 9.9f, 137.9f, 24.8f, 268.8f, -1),
    Capa("mesa_atras", R.drawable.rig_conectando_mesa_atras, 89.3f, 182.0f, 148.8f, 268.8f, -1),
    Capa("e_piernas", R.drawable.rig_conectando_e_piernas, 26.7f, 174.2f, 55.8f, 268.8f, -1),
    Capa("e_cabeza", R.drawable.rig_conectando_e_cabeza, 54.0f, 73.1f, 70.1f, 128.0f, 5),
    Capa("e_torso", R.drawable.rig_conectando_e_torso, 21.1f, 120.6f, 49.6f, 191.3f, -1),
    Capa("mesa", R.drawable.rig_conectando_mesa, 64.5f, 160.3f, 148.8f, 268.8f, -1),
    Capa("portatil", R.drawable.rig_conectando_portatil, 80.0f, 138.6f, 117.8f, 172.7f, -1),
    Capa("e_ante2", R.drawable.rig_conectando_e_ante2, 74.7f, 153.4f, 76.9f, 158.4f, 5),
    Capa("e_ante", R.drawable.rig_conectando_e_ante, 55.2f, 159.3f, 52.7f, 169.6f, 5),
    Capa("tarjeta", 0, 165.8f, 54.6f, 234.1f, 103.3f, -1, 136.7f, 97.4f),
    Capa("t_cabeza", R.drawable.rig_conectando_t_cabeza, 212.3f, 50.5f, 234.4f, 113.2f, 12),
    Capa("t_busto", R.drawable.rig_conectando_t_busto, 188.3f, 109.2f, 234.1f, 152.0f, -1),
    Capa("enlinea", 0, 172.4f, 61.2f, 176.1f, 65.0f, -1, 7.5f, 7.5f),
    Capa("conecta0", 0, 210.9f, 98.7f, 215.5f, 103.3f, -1, 9.1f, 9.1f),
    Capa("conecta1", 0, 229.2f, 98.7f, 233.7f, 103.3f, -1, 9.1f, 9.1f),
    Capa("conecta2", 0, 247.4f, 98.7f, 252.0f, 103.3f, -1, 9.1f, 9.1f)
))

internal val rigCalificar = Rig(267.9f, listOf(
    Capa("e_piernaI", R.drawable.rig_calificar_e_piernai, 13.3f, 149.8f, 26.1f, 142.4f, 4),
    Capa("e_piernaD", R.drawable.rig_calificar_e_piernad, 38.5f, 149.8f, 50.8f, 142.4f, 4),
    Capa("e_pieI", R.drawable.rig_calificar_e_piei, 6.6f, 252.6f, 29.2f, 252.6f, 0),
    Capa("e_pieD", R.drawable.rig_calificar_e_pied, 45.2f, 251.4f, 51.6f, 251.4f, 1),
    Capa("e_pelvis", R.drawable.rig_calificar_e_pelvis, 14.1f, 140.3f, 38.7f, 154.0f, -1),
    Capa("e_cabeza", R.drawable.rig_calificar_e_cabeza, 19.1f, 15.6f, 38.7f, 79.0f, 6),
    Capa("e_torso", R.drawable.rig_calificar_e_torso, 5.4f, 74.8f, 38.9f, 148.6f, 4),
    Capa("e_brazoI", R.drawable.rig_calificar_e_brazoi, 1.7f, 125.0f, 9.7f, 130.8f, 9),
    Capa("e_manoI", R.drawable.rig_calificar_e_manoi, 0.0f, 152.3f, 5.6f, 155.2f, 7),
    Capa("e_mangaI", R.drawable.rig_calificar_e_mangai, 3.7f, 94.7f, 14.3f, 102.8f, 6),
    Capa("e_brazoD", R.drawable.rig_calificar_e_brazod, 61.7f, 125.8f, 67.3f, 131.6f, 12),
    Capa("e_manoD", R.drawable.rig_calificar_e_manod, 66.3f, 154.4f, 71.9f, 157.3f, 10),
    Capa("e_mangaD", R.drawable.rig_calificar_e_mangad, 54.3f, 94.3f, 62.7f, 102.7f, 6),
    Capa("t_piernaI", R.drawable.rig_calificar_t_piernai, 196.6f, 150.7f, 212.6f, 143.2f, 17),
    Capa("t_piernaD", R.drawable.rig_calificar_t_piernad, 225.2f, 151.1f, 237.6f, 143.2f, 17),
    Capa("t_pieI", R.drawable.rig_calificar_t_piei, 189.2f, 258.4f, 209.9f, 258.4f, 13),
    Capa("t_pieD", R.drawable.rig_calificar_t_pied, 234.7f, 258.4f, 241.6f, 258.4f, 14),
    Capa("t_pelvis", R.drawable.rig_calificar_t_pelvis, 198.3f, 141.1f, 225.2f, 154.8f, -1),
    Capa("t_cabeza", R.drawable.rig_calificar_t_cabeza, 205.7f, 15.6f, 225.4f, 71.5f, 19),
    Capa("t_torso", R.drawable.rig_calificar_t_torso, 184.2f, 67.8f, 225.2f, 149.4f, 17),
    Capa("t_brazoI", R.drawable.rig_calificar_t_brazoi, 184.2f, 120.8f, 191.4f, 127.0f, 23),
    Capa("t_manoI", R.drawable.rig_calificar_t_manoi, 182.5f, 149.8f, 188.1f, 152.7f, 20),
    Capa("t_saludo", R.drawable.rig_calificar_t_saludo, 178.8f, 150.3f, 188.1f, 152.7f, 20),
    Capa("t_mangaI", R.drawable.rig_calificar_t_mangai, 184.6f, 89.8f, 195.5f, 98.0f, 19),
    Capa("t_brazoD", R.drawable.rig_calificar_t_brazod, 252.5f, 120.4f, 258.6f, 126.6f, 26),
    Capa("t_manoD", R.drawable.rig_calificar_t_manod, 255.5f, 149.4f, 261.9f, 152.3f, 24),
    Capa("t_mangaD", R.drawable.rig_calificar_t_mangad, 245.9f, 89.3f, 254.6f, 98.0f, 19),
    Capa("estrella0", 0, 68.2f, 21.8f, 80.2f, 33.8f, -1, 24.0f, 24.0f),
    Capa("estrella1", 0, 93.9f, 10.6f, 105.9f, 22.6f, -1, 24.0f, 24.0f),
    Capa("estrella2", 0, 120.1f, 3.6f, 132.1f, 15.6f, -1, 24.0f, 24.0f),
    Capa("estrella3", 0, 146.2f, 10.6f, 158.2f, 22.6f, -1, 24.0f, 24.0f),
    Capa("estrella4", 0, 171.9f, 21.8f, 183.9f, 33.8f, -1, 24.0f, 24.0f)
))
