package br.edu.unijorge.niteconnect;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import br.edu.unijorge.niteconnect.attendance.application.AttendanceAcademicReader;
import static org.assertj.core.api.Assertions.*;

class AttendanceAcademicReaderTest extends AttendanceTestSupport {
    @Autowired AttendanceAcademicReader reader;

    @Test void evidenceSnapshotContainsMeetingPresenceWithoutHourMeasurements() throws Exception {
        UUID event = event();
        UUID attendance = checkIn(event, qr(event));
        UUID period = jdbc.queryForObject("select id from nite_connect.attendance_segments where attendance_id=?", UUID.class, attendance);
        Instant from = Instant.now().minusSeconds(3600), to = Instant.now().plusSeconds(3600);
        var snapshot = reader.read(ADMIN, MEMBER, period, from, to);
        assertThat(snapshot.status()).isEqualTo("REGISTRADA");
        assertThat(snapshot.attributes()).containsKeys("eventId", "eventTitle", "eventKind", "modality", "startsAt", "open")
                .doesNotContainKeys("registeredSeconds", "validatedSeconds", "durationSeconds");
        assertThatThrownBy(() -> reader.read(LEADER, MEMBER, period, from, to))
                .isInstanceOfSatisfying(br.edu.unijorge.niteconnect.shared.api.ApiException.class,
                        error -> assertThat(error.status().value()).isEqualTo(403));
    }

    @Test void canceledMeetingCannotBeCapturedAsAttendanceEvidence() throws Exception {
        UUID event = event();
        UUID attendance = checkIn(event, qr(event));
        UUID period = jdbc.queryForObject("select id from nite_connect.attendance_segments where attendance_id=?", UUID.class, attendance);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/agenda/events/" + event + "/cancel")
                .with(token(ADMIN)).contentType("application/json")
                .content(json.writeValueAsString(java.util.Map.of("expectedVersion", ev(event), "reason", "Cancelamento", "confirmed", true))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        assertThatThrownBy(() -> reader.read(ADMIN, MEMBER, period, Instant.now().minusSeconds(3600), Instant.now().plusSeconds(3600)))
                .isInstanceOf(br.edu.unijorge.niteconnect.shared.api.ApiException.class);
    }
}
