package br.edu.unijorge.niteconnect.shared.api;

import java.util.List;

public record PageResult<T>(List<T> items, int page, int size, long total) {
    public static void validate(int page, int size) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw ApiException.invalid("Paginação inválida: page >= 0 e size entre 1 e 100");
        }
    }
}
