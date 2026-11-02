package br.edu.unijorge.niteconnect.attendance.domain;

import br.edu.unijorge.niteconnect.attendance.domain.Agenda.EventKind;
import br.edu.unijorge.niteconnect.attendance.domain.Agenda.Modality;
import br.edu.unijorge.niteconnect.shared.api.ApiException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.DayOfWeek;
import java.util.Locale;

public final class FrequencyRules {
    public static final ZoneId ZONE = ZoneId.of("America/Bahia");
    private static final Locale PORTUGUESE_BRAZIL = Locale.forLanguageTag("pt-BR");

    private FrequencyRules() {}

    public static boolean isRegular(EventKind kind, Modality modality, Instant startsAt) {
        return kind == EventKind.ENCONTRO && modality == Modality.PRESENCIAL
                && startsAt.atZone(ZONE).getDayOfWeek() == DayOfWeek.THURSDAY;
    }

    public static String sortKey(String displayName) {
        String normalizedSpaces = displayName.strip().replaceAll("\\s+", " ");
        String decomposed = Normalizer.normalize(normalizedSpaces, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").toLowerCase(PORTUGUESE_BRAZIL);
    }

    public static void confirmationWindow(Integer minutes) {
        if (minutes == null || (minutes != 3 && minutes != 5 && minutes != 10)) {
            throw ApiException.invalid("A janela de confirmação deve ser 3, 5 ou 10 minutos");
        }
    }

    public static void range(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to) || to.equals(LocalDate.MAX)) {
            throw ApiException.invalid("Informe from/to em ordem válida");
        }
    }

    public static BigDecimal percentage(long present, long eligibleMeetings) {
        if (eligibleMeetings == 0) return null;
        return BigDecimal.valueOf(present).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(eligibleMeetings), 2, RoundingMode.HALF_UP);
    }

    public static Instant startOf(LocalDate date) {
        return date.atStartOfDay(ZONE).toInstant();
    }

    public static Instant endExclusive(LocalDate inclusiveDate) {
        return inclusiveDate.plusDays(1).atStartOfDay(ZONE).toInstant();
    }

    public static long minutesRemaining(Instant now, Instant eventEndsAt) {
        return ChronoUnit.SECONDS.between(now, eventEndsAt) / 60;
    }
}
