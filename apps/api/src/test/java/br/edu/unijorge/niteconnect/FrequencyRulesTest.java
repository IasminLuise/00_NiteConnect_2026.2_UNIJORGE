package br.edu.unijorge.niteconnect;

import br.edu.unijorge.niteconnect.attendance.domain.Agenda.EventKind;
import br.edu.unijorge.niteconnect.attendance.domain.Agenda.Modality;
import br.edu.unijorge.niteconnect.attendance.domain.FrequencyRules;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class FrequencyRulesTest {
    @Test
    void regularMeetingRequiresInPersonEncontroBeginningOnLocalThursday() {
        var thursday = LocalDate.of(2026, 11, 5).atTime(LocalTime.of(20, 0)).atZone(FrequencyRules.ZONE).toInstant();
        var friday = LocalDate.of(2026, 11, 6).atTime(LocalTime.of(20, 0)).atZone(FrequencyRules.ZONE).toInstant();
        assertThat(FrequencyRules.isRegular(EventKind.ENCONTRO, Modality.PRESENCIAL, thursday)).isTrue();
        assertThat(FrequencyRules.isRegular(EventKind.ENCONTRO, Modality.REMOTA, thursday)).isFalse();
        assertThat(FrequencyRules.isRegular(EventKind.EVENTO, Modality.PRESENCIAL, thursday)).isFalse();
        assertThat(FrequencyRules.isRegular(EventKind.ENCONTRO, Modality.PRESENCIAL, friday)).isFalse();
    }
}
