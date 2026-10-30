package br.edu.unijorge.niteconnect.analytics.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class AnalyticsContracts {
    private AnalyticsContracts() {}
    public enum ReportType { FREQUENCIA_PARTICIPACAO, ATIVIDADES_ENTREGAS, ACOMPANHAMENTO_PROJETOS, GERENCIAL_CONSOLIDADO }
    public enum ExportFormat { CSV, PDF }
    public record Report(ReportType type, LocalDate from, LocalDate to, Instant generatedAt,
                         String criteria, List<String> columns, List<List<String>> rows,
                         int page, int size, long total, List<String> unavailableMetrics) {}
    public record Dashboard(LocalDate from, LocalDate to, Instant generatedAt, String scope,
                            Map<String, Long> indicators, List<String> columns,List<List<String>> rows,
                            List<String> unavailableMetrics) {}
    public record HistoricalAnalysis(LocalDate from, LocalDate to, Instant generatedAt, String criteria,
                                     List<String> columns, List<List<String>> rows, int page, int size, long total) {}
    public record MetricComparison(String metric, long baselineValue, long comparisonValue, long delta) {}
    public record Comparison(LocalDate baselineFrom,LocalDate baselineTo,LocalDate comparisonFrom,LocalDate comparisonTo,
                             Instant generatedAt,String scope,List<MetricComparison> metrics,List<String> unavailableMetrics) {}
}
