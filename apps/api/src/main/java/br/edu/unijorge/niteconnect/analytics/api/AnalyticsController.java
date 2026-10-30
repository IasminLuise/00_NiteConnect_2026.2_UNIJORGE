package br.edu.unijorge.niteconnect.analytics.api;

import br.edu.unijorge.niteconnect.analytics.api.AnalyticsContracts.*;
import br.edu.unijorge.niteconnect.analytics.application.AnalyticsExporter;
import br.edu.unijorge.niteconnect.analytics.application.AnalyticsService;
import br.edu.unijorge.niteconnect.identity.application.IdentityDirectory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
public class AnalyticsController {
    private final AnalyticsService analytics;
    private final AnalyticsExporter exporter;
    private final IdentityDirectory identities;
    public AnalyticsController(AnalyticsService analytics,AnalyticsExporter exporter,IdentityDirectory identities) {
        this.analytics=analytics;this.exporter=exporter;this.identities=identities;
    }

    @GetMapping("/api/v1/me/dashboard")
    Dashboard ownDashboard(@AuthenticationPrincipal Jwt jwt,@RequestParam LocalDate from,@RequestParam LocalDate to) {
        return analytics.ownDashboard(identities.member(jwt),from,to);
    }
    @GetMapping("/api/v1/projects/{projectId}/dashboard")
    Dashboard projectDashboard(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID projectId,@RequestParam LocalDate from,@RequestParam LocalDate to) {
        return analytics.projectDashboard(identities.member(jwt),projectId,from,to);
    }
    @GetMapping("/api/v1/analytics/dashboard")
    Dashboard administrativeDashboard(@AuthenticationPrincipal Jwt jwt,@RequestParam LocalDate from,@RequestParam LocalDate to) {
        return analytics.administrativeDashboard(identities.member(jwt),from,to);
    }
    @GetMapping("/api/v1/analytics/history")
    HistoricalAnalysis administrativeHistory(@AuthenticationPrincipal Jwt jwt,@RequestParam LocalDate from,@RequestParam LocalDate to,
                                             @RequestParam(required=false) UUID projectId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size) {
        return analytics.administrativeHistory(identities.member(jwt),from,to,projectId,page,size);
    }
    @GetMapping("/api/v1/projects/{projectId}/analytics/history")
    HistoricalAnalysis projectHistory(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID projectId,@RequestParam LocalDate from,@RequestParam LocalDate to,
                                      @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size) {
        return analytics.projectHistory(identities.member(jwt),projectId,from,to,page,size);
    }
    @GetMapping("/api/v1/me/analytics/comparison")
    Comparison ownComparison(@AuthenticationPrincipal Jwt jwt,@RequestParam LocalDate baselineFrom,@RequestParam LocalDate baselineTo,
                             @RequestParam LocalDate comparisonFrom,@RequestParam LocalDate comparisonTo,@RequestParam(required=false) UUID projectId) {
        return analytics.ownComparison(identities.member(jwt),projectId,baselineFrom,baselineTo,comparisonFrom,comparisonTo);
    }
    @GetMapping("/api/v1/projects/{projectId}/analytics/comparison")
    Comparison projectComparison(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID projectId,
                                 @RequestParam LocalDate baselineFrom,@RequestParam LocalDate baselineTo,
                                 @RequestParam LocalDate comparisonFrom,@RequestParam LocalDate comparisonTo) {
        return analytics.projectComparison(identities.member(jwt),projectId,baselineFrom,baselineTo,comparisonFrom,comparisonTo);
    }
    @GetMapping("/api/v1/analytics/comparison")
    Comparison administrativeComparison(@AuthenticationPrincipal Jwt jwt,@RequestParam LocalDate baselineFrom,@RequestParam LocalDate baselineTo,
                                        @RequestParam LocalDate comparisonFrom,@RequestParam LocalDate comparisonTo,
                                        @RequestParam(required=false) UUID projectId,@RequestParam(required=false) UUID memberId) {
        return analytics.administrativeComparison(identities.member(jwt),projectId,memberId,baselineFrom,baselineTo,comparisonFrom,comparisonTo);
    }

    @GetMapping("/api/v1/me/reports/{type}")
    Report ownReport(@AuthenticationPrincipal Jwt jwt,@PathVariable ReportType type,@RequestParam LocalDate from,@RequestParam LocalDate to,
                     @RequestParam(required=false) UUID projectId,@RequestParam(required=false) String situation,
                     @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size) {
        return analytics.ownReport(identities.member(jwt),type,from,to,projectId,situation,page,size);
    }
    @GetMapping("/api/v1/projects/{projectId}/reports/{type}")
    Report projectReport(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID projectId,@PathVariable ReportType type,
                         @RequestParam LocalDate from,@RequestParam LocalDate to,@RequestParam(required=false) String situation,
                         @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size) {
        return analytics.projectReport(identities.member(jwt),projectId,type,from,to,situation,page,size);
    }
    @GetMapping("/api/v1/analytics/reports/{type}")
    Report administrativeReport(@AuthenticationPrincipal Jwt jwt,@PathVariable ReportType type,@RequestParam LocalDate from,@RequestParam LocalDate to,
                                @RequestParam(required=false) UUID projectId,@RequestParam(required=false) UUID memberId,
                                @RequestParam(required=false) String situation,
                                @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size) {
        return analytics.administrativeReport(identities.member(jwt),type,from,to,projectId,memberId,situation,page,size,false);
    }

    @GetMapping("/api/v1/analytics/reports/{type}/export")
    ResponseEntity<byte[]> administrativeExport(@AuthenticationPrincipal Jwt jwt,@PathVariable ReportType type,@RequestParam LocalDate from,@RequestParam LocalDate to,
                                @RequestParam ExportFormat format,@RequestParam(required=false) UUID projectId,@RequestParam(required=false) UUID memberId,
                                @RequestParam(required=false) String situation) {
        Report report=analytics.administrativeReport(identities.member(jwt),type,from,to,projectId,memberId,situation,0,200,true);
        return export(report,format);
    }
    @GetMapping("/api/v1/projects/{projectId}/reports/{type}/export")
    ResponseEntity<byte[]> projectExport(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID projectId,@PathVariable ReportType type,
                                          @RequestParam LocalDate from,@RequestParam LocalDate to,@RequestParam ExportFormat format,
                                          @RequestParam(required=false) String situation) {
        Report report=analytics.projectExport(identities.member(jwt),projectId,type,from,to,situation);
        return export(report,format);
    }
    @GetMapping("/api/v1/me/reports/{type}/export")
    ResponseEntity<byte[]> ownExport(@AuthenticationPrincipal Jwt jwt,@PathVariable ReportType type,@RequestParam LocalDate from,@RequestParam LocalDate to,
                                     @RequestParam(required=false) UUID projectId,@RequestParam ExportFormat format,
                                     @RequestParam(required=false) String situation) {
        Report report=analytics.ownExport(identities.member(jwt),type,from,to,projectId,situation);
        return export(report,format);
    }

    private ResponseEntity<byte[]> export(Report report,ExportFormat format) {
        byte[] bytes=format==ExportFormat.CSV?exporter.csv(report):exporter.pdf(report);
        String file="nite-connect-"+report.type().name().toLowerCase()+"."+format.name().toLowerCase();
        MediaType media=format==ExportFormat.CSV?new MediaType("text","csv",java.nio.charset.StandardCharsets.UTF_8):MediaType.APPLICATION_PDF;
        return ResponseEntity.ok().contentType(media).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+file+"\"").body(bytes);
    }
}
