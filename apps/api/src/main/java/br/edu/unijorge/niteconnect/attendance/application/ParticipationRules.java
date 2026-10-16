package br.edu.unijorge.niteconnect.attendance.application;

import br.edu.unijorge.niteconnect.shared.api.ApiException;
import java.time.*;

public final class ParticipationRules {
    private ParticipationRules() {}
    public static void interval(Instant start,Instant end) {
        if(start==null || end==null || !end.isAfter(start)) throw ApiException.invalid("Intervalo deve possuir início anterior ao término");
    }
    public static void past(Instant start,Instant end,Clock clock) { interval(start,end);if(end.isAfter(clock.instant())) throw ApiException.invalid("Período ainda não ocorreu"); }
    public static void confirm(Boolean confirmed) { if(!Boolean.TRUE.equals(confirmed)) throw ApiException.invalid("Confirmação explícita obrigatória"); }
    public static void reason(String reason) { if(reason==null || reason.isBlank() || reason.length()>1000) throw ApiException.invalid("Motivo obrigatório até 1000 caracteres"); }
    public static long clippedSeconds(Instant start,Instant end,Instant from,Instant to) {
        if(end==null) return 0;
        Instant a=start.isBefore(from)?from:start,b=end.isAfter(to)?to:end;
        return b.isAfter(a)?Duration.between(a,b).getSeconds():0;
    }
}
