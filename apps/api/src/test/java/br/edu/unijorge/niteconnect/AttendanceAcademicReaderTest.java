package br.edu.unijorge.niteconnect;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import br.edu.unijorge.niteconnect.attendance.application.AttendanceAcademicReader;
import static org.assertj.core.api.Assertions.*;

class AttendanceAcademicReaderTest extends AttendanceTestSupport {
    @Autowired AttendanceAcademicReader reader;

    @Test void frequencyConfirmationIsAnAdditiveAcademicEvidenceSourceWithoutHourMeasurements() throws Exception {
        UUID event = event(), list = openList(event);
        UUID attendance = checkIn(event, qrForList(list));
        Instant from = Instant.now().minusSeconds(3600), to = Instant.now().plusSeconds(3600);
        var snapshot = reader.readConfirmation(ADMIN, MEMBER, attendance, from, to);
        assertThat(snapshot.status()).isEqualTo("REGISTRADA");
        assertThat(snapshot.attributes()).containsKeys("eventId", "eventTitle", "eventKind", "modality", "startsAt", "confirmedAt", "listId")
                .doesNotContainKeys("registeredSeconds", "validatedSeconds", "durationSeconds");
        assertThatThrownBy(() -> reader.readConfirmation(LEADER, MEMBER, attendance, from, to))
                .isInstanceOfSatisfying(br.edu.unijorge.niteconnect.shared.api.ApiException.class,
                        error -> assertThat(error.status().value()).isEqualTo(403));
        var otherPresenceChange = json.writeValueAsString(Map.of("expectedVersion",
                jdbc.queryForObject("select version from nite_connect.attendance_lists where id=?", Long.class,
                        UUID.fromString(snapshot.attributes().get("listId"))),
                "targetStatus", "JUSTIFICADO", "reason", "EMAIL_EXTERNO_RECEBIDO"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                        "/api/v1/attendance/lists/" + snapshot.attributes().get("listId") + "/participants/" + OTHER)
                .with(token(ADMIN)).contentType("application/json").content(otherPresenceChange))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        assertThat(reader.readConfirmation(ADMIN, MEMBER, attendance, from, to).revision()).isEqualTo(snapshot.revision());
    }

    @Test void legacyAttendanceSegmentsRemainReadableByTheExistingAcademicSource() throws Exception {
        UUID event = event();
        UUID attendance = legacyAttendance(event, MEMBER);
        UUID segment = jdbc.queryForObject("select id from nite_connect.attendance_segments where attendance_id=?", UUID.class, attendance);
        Instant from = Instant.now().minusSeconds(3600), to = Instant.now().plusSeconds(3600);
        var snapshot = reader.read(ADMIN, MEMBER, segment, from, to);
        assertThat(snapshot.status()).isEqualTo("REGISTRADA");
        assertThat(snapshot.attributes()).containsKeys("eventId", "eventTitle", "eventKind", "modality", "startsAt", "open");
    }

    @Test void canceledFrequencyMeetingCannotBeCapturedAsAttendanceEvidence() throws Exception {
        UUID event = event(), list = openList(event);
        UUID attendance = checkIn(event, qrForList(list));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/agenda/events/" + event + "/cancel")
                .with(token(ADMIN)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("expectedVersion", ev(event), "reason", "Cancelamento", "confirmed", true))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        assertThatThrownBy(() -> reader.readConfirmation(ADMIN, MEMBER, attendance,
                Instant.now().minusSeconds(3600), Instant.now().plusSeconds(3600)))
                .isInstanceOf(br.edu.unijorge.niteconnect.shared.api.ApiException.class);
    }

    @Test void canceledLegacyMeetingCannotBeCapturedAsSegmentEvidence() throws Exception {
        UUID event = event();
        UUID attendance = legacyAttendance(event, MEMBER);
        UUID segment = jdbc.queryForObject("select id from nite_connect.attendance_segments where attendance_id=?", UUID.class, attendance);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/agenda/events/" + event + "/cancel")
                .with(token(ADMIN)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("expectedVersion", ev(event), "reason", "Cancelamento", "confirmed", true))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        assertThatThrownBy(() -> reader.read(ADMIN, MEMBER, segment, Instant.now().minusSeconds(3600), Instant.now().plusSeconds(3600)))
                .isInstanceOf(br.edu.unijorge.niteconnect.shared.api.ApiException.class);
    }
}
