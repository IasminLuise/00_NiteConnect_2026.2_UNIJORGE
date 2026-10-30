package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WeeklyParticipationTest extends ProjectTestSupport {
    @Test
    void personalWeeklySummaryReturnsSeparateApprovedLoads() throws Exception {
        mvc.perform(get("/api/v1/me/participation-weekly-summary")
                        .with(token(MEMBER)).param("from", "2026-10-26").param("to", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value("2026-10-26"))
                .andExpect(jsonPath("$.modalities.presential.requiredSeconds").value(18000))
                .andExpect(jsonPath("$.modalities.remote.requiredSeconds").value(14400))
                .andExpect(jsonPath("$.modalities.presential.remainingSeconds").value(18000))
                .andExpect(jsonPath("$.modalities.remote.remainingSeconds").value(14400));
    }

    @Test
    void weeklySummaryRejectsImplicitOrNonSevenDayPeriods() throws Exception {
        mvc.perform(get("/api/v1/me/participation-weekly-summary")
                        .with(token(MEMBER)).param("from", "2026-10-26").param("to", "2026-11-02"))
                .andExpect(status().isBadRequest());
    }
}
