package com.grupo3.nexo.tutores;

import java.util.List;

import com.grupo3.nexo.error.ErrorNegocio;

/** `Paginacion` del contrato más sus `elementos`. */
public record PaginaDto<T>(int pagina, int tamano, long totalElementos, int totalPaginas, List<T> elementos) {

    private static final int TAMANO_MAXIMO = 50;

    static <T> PaginaDto<T> de(List<T> elementos, int pagina, int tamano, long total) {
        return new PaginaDto<>(pagina, tamano, total, (int) ((total + tamano - 1) / tamano), elementos);
    }

    /** El límite del desplazamiento evita un 500: la consulta lo recibe como int. */
    static void validar(int pagina, int tamano) {
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw ErrorNegocio.datosInvalidos("tamano", "debe estar entre 1 y " + TAMANO_MAXIMO);
        }
        if (pagina < 0 || (long) pagina * tamano > Integer.MAX_VALUE) {
            throw ErrorNegocio.datosInvalidos("pagina", "no es una página válida");
        }
    }
}
