package br.edu.unijorge.niteconnect;

import br.edu.unijorge.niteconnect.analytics.api.AnalyticsContracts.Report;
import br.edu.unijorge.niteconnect.analytics.api.AnalyticsContracts.ReportType;
import br.edu.unijorge.niteconnect.analytics.application.AnalyticsExporter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsExporterTest {
    private final AnalyticsExporter exporter=new AnalyticsExporter();
    private final Report report=new Report(ReportType.ATIVIDADES_ENTREGAS,LocalDate.parse("2026-10-01"),LocalDate.parse("2026-10-31"),
            Instant.parse("2026-10-30T12:00:00Z"),"Período e fuso definidos",List.of("activityId","title"),
            List.of(List.of("source-id","=HYPERLINK(\"https://example.invalid\")")),0,50,1,List.of("B05 indisponível"));
    @Test void csvEscapesDelimitersQuotesAndSpreadsheetFormulas() {
        String value=new String(exporter.csv(report),java.nio.charset.StandardCharsets.UTF_8);
        assertThat(value).contains("\"'=HYPERLINK(\"\"https://example.invalid\"\")\"");
        assertThat(value).contains("\"Período e fuso definidos\"");
    }
    @Test void pdfContainsPeriodCriteriaAndSourceRow() throws Exception {
        try(var document=Loader.loadPDF(exporter.pdf(report))) {
            String text=new PDFTextStripper().getText(document);
            assertThat(text).contains("2026-10-01 a 2026-10-31","Período e fuso definidos","source-id","B05 indisponível");
        }
    }
}
