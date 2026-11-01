package br.edu.unijorge.niteconnect.attendance.application;

import br.edu.unijorge.niteconnect.shared.api.ApiException;

import java.time.Instant;

public final class AgendaRules {
    private AgendaRules() {}

    public static void interval(Instant start, Instant end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw ApiException.invalid("Intervalo deve possuir início anterior ao término");
        }
    }

    public static void confirm(Boolean confirmed) {
        if (!Boolean.TRUE.equals(confirmed)) throw ApiException.invalid("Confirmação explícita obrigatória");
    }
}
