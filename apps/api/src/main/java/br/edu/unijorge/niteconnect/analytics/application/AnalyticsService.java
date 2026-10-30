package br.edu.unijorge.niteconnect.analytics.application;

import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.access.application.ProjectAccess;
import br.edu.unijorge.niteconnect.analytics.api.AnalyticsContracts.*;
import br.edu.unijorge.niteconnect.analytics.infrastructure.AnalyticsRepository;
import br.edu.unijorge.niteconnect.projecthealth.application.ProjectHealthService;
import br.edu.unijorge.niteconnect.projects.application.ProjectDirectory;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static br.edu.unijorge.niteconnect.projecthealth.api.ProjectHealthContracts.Health;
import br.edu.unijorge.niteconnect.analytics.api.AnalyticsContracts.HistoricalAnalysis;
import br.edu.unijorge.niteconnect.analytics.api.AnalyticsContracts.Comparison;
import br.edu.unijorge.niteconnect.analytics.api.AnalyticsContracts.MetricComparison;

@Service
public class AnalyticsService {
    private static final List<String> B05_UNAVAILABLE = List.of("Evidências documentais e Diários de Bordo: indisponíveis (não há serviço Java B05 ativo)",
            "Estados históricos completos de projeto/atividade: indisponíveis quando não preservados como eventos ou snapshots na origem");
    private static final List<String> FREQUENCY_COLUMNS = List.of("periodId","memberName","modality","kind","reviewStatus","startsAt","endsAt","secondsInRange");
    private static final List<String> ACTIVITY_COLUMNS = List.of("activityId","project","activity","executor","status","createdAt","dueOn","completedAt","overdueNow");
    private final AnalyticsRepository repository;
    private final AccessService access;
    private final ProjectAccess projectAccess;
    private final ProjectDirectory projects;
    private final ProjectHealthService health;
    private final Clock clock;

    public AnalyticsService(AnalyticsRepository repository, AccessService access, ProjectAccess projectAccess,
                            ProjectDirectory projects, ProjectHealthService health, Clock clock) {
        this.repository=repository; this.access=access; this.projectAccess=projectAccess; this.projects=projects; this.health=health; this.clock=clock;
    }

    @Transactional(readOnly=true)
    public Report ownReport(UUID actor, ReportType type, LocalDate from, LocalDate to, UUID projectId,String situation,int page, int size) {
        access.requireActive(actor);
        if (projectId != null) projectAccess.requireRead(actor, projectId);
        return build(actor,type,from,to,projectId,actor,situation,page,size,false);
    }

    @Transactional(readOnly=true)
    public Report ownExport(UUID actor, ReportType type, LocalDate from, LocalDate to, UUID projectId,String situation) {
        access.requireActive(actor);
        if (projectId != null) projectAccess.requireRead(actor, projectId);
        return build(actor,type,from,to,projectId,actor,situation,0,200,true);
    }

    @Transactional(readOnly=true)
    public Report projectReport(UUID actor, UUID projectId, ReportType type, LocalDate from, LocalDate to,String situation,int page, int size) {
        projectAccess.requireManage(actor, projectId);
        return build(actor,type,from,to,projectId,null,situation,page,size,false);
    }

    @Transactional(readOnly=true)
    public Report projectExport(UUID actor, UUID projectId, ReportType type, LocalDate from, LocalDate to,String situation) {
        projectAccess.requireManage(actor, projectId);
        return build(actor,type,from,to,projectId,null,situation,0,200,true);
    }

    @Transactional(readOnly=true)
    public Report administrativeReport(UUID actor, ReportType type, LocalDate from, LocalDate to, UUID projectId, UUID memberId,
                                       String situation,int page, int size, boolean export) {
        access.requireAdministrator(actor);
        if (projectId != null) projects.require(projectId);
        if (memberId != null) access.requireActive(memberId);
        return build(actor,type,from,to,projectId,memberId,situation,page,size,export);
    }

    @Transactional(readOnly=true)
    public Dashboard ownDashboard(UUID actor, LocalDate from, LocalDate to) {
        access.requireActive(actor);
        Report report=build(actor,ReportType.FREQUENCIA_PARTICIPACAO,from,to,null,actor,null,0,200,true);
        Report activities=build(actor,ReportType.ATIVIDADES_ENTREGAS,from,to,null,actor,null,0,200,true);
        var result=dashboard(from,to,"integrante",report);
        var indicators=new LinkedHashMap<>(result.indicators());
        indicators.put("activities",activities.total());
        for(String status:List.of("PLANEJADA","EM_EXECUCAO","CONCLUIDA","CANCELADA"))
            indicators.put("activities."+status.toLowerCase(),activities.rows().stream().filter(row->row.get(4).equals(status)).count());
        indicators.put("overdueActivitiesNow",activities.rows().stream().filter(row->row.get(8).equals("true")).count());
        var unavailable=new ArrayList<>(result.unavailableMetrics());
        activities.unavailableMetrics().stream().filter(value->!unavailable.contains(value)).forEach(unavailable::add);
        return new Dashboard(from,to,result.generatedAt(),result.scope(),Map.copyOf(indicators),activities.columns(),activities.rows(),List.copyOf(unavailable));
    }

    @Transactional(readOnly=true)
    public Dashboard projectDashboard(UUID actor, UUID projectId, LocalDate from, LocalDate to) {
        projectAccess.requireManage(actor,projectId);
        Report report=build(actor,ReportType.GERENCIAL_CONSOLIDADO,from,to,projectId,null,null,0,200,true);
        Report participation=build(actor,ReportType.FREQUENCIA_PARTICIPACAO,from,to,projectId,null,null,0,200,true);
        var result=dashboard(from,to,"liderança do projeto",report);
        var indicators=new LinkedHashMap<>(result.indicators());
        addParticipationIndicators(indicators,participation);
        var unavailable=new ArrayList<>(result.unavailableMetrics());
        participation.unavailableMetrics().stream().filter(value->!unavailable.contains(value)).forEach(unavailable::add);
        unavailable.add("Presenças em eventos sem vínculo de projeto não são atribuídas a este painel");
        return new Dashboard(from,to,result.generatedAt(),result.scope(),Map.copyOf(indicators),report.columns(),report.rows(),List.copyOf(unavailable));
    }

    @Transactional(readOnly=true)
    public Dashboard administrativeDashboard(UUID actor, LocalDate from, LocalDate to) {
        access.requireAdministrator(actor);
        Report report=build(actor,ReportType.GERENCIAL_CONSOLIDADO,from,to,null,null,null,0,200,true);
        Report participation=build(actor,ReportType.FREQUENCIA_PARTICIPACAO,from,to,null,null,null,0,200,true);
        var result=dashboard(from,to,"Gestão Geral/Coordenação",report);
        var indicators=new LinkedHashMap<>(result.indicators());
        addParticipationIndicators(indicators,participation);
        return new Dashboard(from,to,result.generatedAt(),result.scope(),Map.copyOf(indicators),report.columns(),report.rows(),result.unavailableMetrics());
    }

    @Transactional(readOnly=true)
    public HistoricalAnalysis projectHistory(UUID actor,UUID projectId,LocalDate from,LocalDate to,int page,int size) {
        projectAccess.requireManage(actor,projectId);
        return history(projectId,from,to,page,size);
    }

    @Transactional(readOnly=true)
    public HistoricalAnalysis administrativeHistory(UUID actor,LocalDate from,LocalDate to,UUID projectId,int page,int size) {
        access.requireAdministrator(actor);
        if(projectId!=null) projects.require(projectId);
        return history(projectId,from,to,page,size);
    }

    @Transactional(readOnly=true)
    public Comparison ownComparison(UUID actor,UUID projectId,LocalDate baselineFrom,LocalDate baselineTo,LocalDate comparisonFrom,LocalDate comparisonTo) {
        access.requireActive(actor);
        if(projectId!=null) projectAccess.requireRead(actor,projectId);
        return comparison(projectId,actor,baselineFrom,baselineTo,comparisonFrom,comparisonTo,"integrante");
    }

    @Transactional(readOnly=true)
    public Comparison projectComparison(UUID actor,UUID projectId,LocalDate baselineFrom,LocalDate baselineTo,LocalDate comparisonFrom,LocalDate comparisonTo) {
        projectAccess.requireManage(actor,projectId);
        return comparison(projectId,null,baselineFrom,baselineTo,comparisonFrom,comparisonTo,"liderança do projeto");
    }

    @Transactional(readOnly=true)
    public Comparison administrativeComparison(UUID actor,UUID projectId,UUID memberId,LocalDate baselineFrom,LocalDate baselineTo,
                                               LocalDate comparisonFrom,LocalDate comparisonTo) {
        access.requireAdministrator(actor);
        if(projectId!=null) projects.require(projectId);
        if(memberId!=null) access.requireActive(memberId);
        return comparison(projectId,memberId,baselineFrom,baselineTo,comparisonFrom,comparisonTo,"Gestão Geral/Coordenação");
    }

    private Comparison comparison(UUID projectId,UUID memberId,LocalDate baselineFrom,LocalDate baselineTo,
                                  LocalDate comparisonFrom,LocalDate comparisonTo,String scope) {
        validateWindow(baselineFrom,baselineTo);validateWindow(comparisonFrom,comparisonTo);
        long baselineDays=ChronoUnit.DAYS.between(baselineFrom,baselineTo);
        long comparisonDays=ChronoUnit.DAYS.between(comparisonFrom,comparisonTo);
        if(baselineDays!=comparisonDays || !(baselineTo.isBefore(comparisonFrom)||comparisonTo.isBefore(baselineFrom)))
            throw ApiException.invalid("As janelas devem ter a mesma duração inclusiva e não podem se sobrepor");
        ZoneId zone=clock.getZone();
        Instant baselineStart=baselineFrom.atStartOfDay(zone).toInstant();
        Instant baselineEnd=baselineTo.plusDays(1).atStartOfDay(zone).toInstant();
        Instant comparisonStart=comparisonFrom.atStartOfDay(zone).toInstant();
        Instant comparisonEnd=comparisonTo.plusDays(1).atStartOfDay(zone).toInstant();
        var before=repository.comparisonMetrics(projectId,memberId,baselineStart,baselineEnd);
        var current=repository.comparisonMetrics(projectId,memberId,comparisonStart,comparisonEnd);
        var metrics=new ArrayList<MetricComparison>();
        for(String name:before.keySet()) metrics.add(new MetricComparison(name,before.get(name),current.get(name),Math.subtractExact(current.get(name),before.get(name))));
        return new Comparison(baselineFrom,baselineTo,comparisonFrom,comparisonTo,clock.instant(),scope,List.copyOf(metrics),
                List.of("Contagens de eventos de origem; não equivalem a indicadores de desempenho homologados.",
                        "A consulta compara criação de atividades, eventos preservados e início de períodos; não reconstitui estados completos nem atribui horas pela conclusão de tarefas."));
    }

    private void validateWindow(LocalDate from,LocalDate to) {
        if(from==null||to==null||from.isAfter(to)||ChronoUnit.DAYS.between(from,to)>365||to.equals(LocalDate.MAX))
            throw ApiException.invalid("Cada janela de comparação deve ter até 366 dias inclusivos e datas válidas");
    }

    private HistoricalAnalysis history(UUID projectId,LocalDate from,LocalDate to,int page,int size) {
        validate(from,to,page,size,false);
        Instant start=from.atStartOfDay(clock.getZone()).toInstant();
        Instant end=to.plusDays(1).atStartOfDay(clock.getZone()).toInstant();
        var all=repository.historicalEvents(projectId,start,end);
        int first=Math.min(page*size,all.size()),last=Math.min(first+size,all.size());
        return new HistoricalAnalysis(from,to,clock.instant(),"Eventos preservados pelas origens; estados não são reconstruídos para períodos sem eventos/snapshots.",
                List.of("sourceType","sourceId","projectId","project","action","occurredAt","previousState","currentState"),
                List.copyOf(all.subList(first,last)),page,size,repository.countHistoricalEvents(projectId,start,end));
    }

    private Dashboard dashboard(LocalDate from,LocalDate to,String scope,Report report) {
        var indicators=new LinkedHashMap<String,Long>();
        indicators.put("recordsInPeriod",report.total());
        if(report.type()!=ReportType.FREQUENCIA_PARTICIPACAO) {
            indicators.put("projects",(long)report.rows().size());
            indicators.put("activities",report.rows().stream().mapToLong(row->parse(row,3)).sum());
            indicators.put("completedActivities",report.rows().stream().mapToLong(row->parse(row,5)).sum());
            indicators.put("openActivities",report.rows().stream().mapToLong(row->parse(row,7)).sum());
            indicators.put("overdueActivitiesNow",report.rows().stream().mapToLong(row->parse(row,4)).sum());
        }
        if (report.type()==ReportType.FREQUENCIA_PARTICIPACAO) {
            addParticipationIndicators(indicators,report);
        }
        return new Dashboard(from,to,report.generatedAt(),scope,Map.copyOf(indicators),report.columns(),report.rows(),report.unavailableMetrics());
    }

    private void addParticipationIndicators(Map<String,Long> indicators,Report report) {
        for(String status:List.of("PENDENTE","APROVADA","RECUSADA"))
            indicators.put("participation."+status.toLowerCase(),report.rows().stream().filter(row->row.get(4).equals(status)).count());
        for(String modality:List.of("PRESENCIAL","REMOTA"))
            indicators.put("participation."+modality.toLowerCase()+".seconds",report.rows().stream().filter(row->row.get(2).equals(modality)).mapToLong(row->Long.parseLong(row.get(7))).sum());
        long validated=report.rows().stream().filter(row->row.get(4).equals("APROVADA")).mapToLong(row->Long.parseLong(row.get(7))).sum();
        indicators.put("validatedSeconds",validated);
    }

    private Report build(UUID actor,ReportType type,LocalDate from,LocalDate to,UUID projectId,UUID memberId,String situation,
                         int page,int size,boolean export) {
        validate(from,to,page,size,export);
        validateSituation(type,situation);
        ZoneId zone=clock.getZone();
        Instant start=from.atStartOfDay(zone).toInstant();
        Instant end=to.plusDays(1).atStartOfDay(zone).toInstant();
        Instant generated=clock.instant();
        List<String> columns;
        List<List<String>> all;
        long total;
        List<String> unavailable=new ArrayList<>(B05_UNAVAILABLE);
        switch(type) {
            case FREQUENCIA_PARTICIPACAO -> { columns=FREQUENCY_COLUMNS; all=repository.participation(projectId,memberId,start,end,situation); total=repository.countParticipation(projectId,memberId,start,end,situation); }
            case ATIVIDADES_ENTREGAS -> { columns=ACTIVITY_COLUMNS; all=repository.activities(projectId,memberId,start,end,situation); total=repository.countActivities(projectId,memberId,start,end,situation); }
            case ACOMPANHAMENTO_PROJETOS, GERENCIAL_CONSOLIDADO -> {
                var source=repository.projects(projectId,memberId,start,end,situation);
                total=repository.countProjects(projectId,memberId,start,end,situation);
                columns=new ArrayList<>(List.of("projectId","project","officialStatus","activities","overdueActivitiesNow","completedActivities","activeMembers","openActivities","healthClassification","healthReasons"));
                all=new ArrayList<>();
                for(var row:source) {
                    Health classification=health.health(actor,UUID.fromString(row.get(0)));
                    all.add(List.of(row.get(0),row.get(1),row.get(2),row.get(3),row.get(4),row.get(5),row.get(6),row.get(7),classification.status().name(),String.join(" ",classification.reasons())));
                }
                if(type==ReportType.ACOMPANHAMENTO_PROJETOS) unavailable.add("Estados do projeto e composição da equipe são atuais no momento da consulta; a origem não preserva snapshots completos para reconstrução histórica");
                else unavailable.add("Notas acadêmicas homologadas não integram este relatório gerencial para preservar o escopo e a autorização próprios do Bloco 06");
            }
            default -> throw ApiException.invalid("Modelo de relatório não suportado");
        }
        if(type==ReportType.FREQUENCIA_PARTICIPACAO && projectId!=null)
            unavailable.add("Registros de presença sem atividade vinculada não possuem relação de projeto e são omitidos desta consulta");
        if(type==ReportType.GERENCIAL_CONSOLIDADO && memberId!=null)
            unavailable.add("A situação atual do projeto e das atividades não é reconstruída para a data de cada vínculo histórico");
        int sourceLimit=switch(type) {
            case FREQUENCIA_PARTICIPACAO -> 2000;
            case ATIVIDADES_ENTREGAS -> 1000;
            case ACOMPANHAMENTO_PROJETOS,GERENCIAL_CONSOLIDADO -> 200;
        };
        if(total>all.size()) unavailable.add("A consulta retornou as primeiras "+sourceLimit+" linhas de "+total+"; o resultado está truncado. Reduza o período ou aplique filtro de projeto/integrante.");
        if(type==ReportType.GERENCIAL_CONSOLIDADO) unavailable.add("Atividades independentes, sem projeto, são consultadas no modelo Atividades e Entregas e não entram nos totais por projeto");
        int first=export?0:Math.min(page*size,all.size());
        int last=export?all.size():Math.min(first+size,all.size());
        List<List<String>> rows=List.copyOf(all.subList(first,last));
        String criteria="Período inclusivo "+from+" a "+to+"; fuso "+zone+"; registros criados/iniciados dentro do período; situação="+(situation==null?"todas":situation)+"; valores de estado operacional avaliados na geração "+generated+".";
        return new Report(type,from,to,generated,criteria,List.copyOf(columns),rows,page,size,total,List.copyOf(unavailable));
    }

    private void validate(LocalDate from,LocalDate to,int page,int size,boolean export) {
        if(from==null || to==null || from.isAfter(to) || ChronoUnit.DAYS.between(from,to)>365) throw ApiException.invalid("Informe um período de até 366 dias, com início não posterior ao fim");
        if(page<0 || size<1 || size>200 || (!export && (long)page*size>100_000)) throw ApiException.invalid("Paginação inválida");
    }
    private void validateSituation(ReportType type,String situation) {
        if(situation==null)return;
        List<String> allowed=switch(type) {
            case FREQUENCIA_PARTICIPACAO -> List.of("PENDENTE","APROVADA","RECUSADA");
            case ATIVIDADES_ENTREGAS -> List.of("PLANEJADA","EM_EXECUCAO","CONCLUIDA","CANCELADA");
            case ACOMPANHAMENTO_PROJETOS,GERENCIAL_CONSOLIDADO -> List.of("PLANEJAMENTO","ATIVO","PAUSADO","CONCLUIDO");
        };
        if(!allowed.contains(situation)) throw ApiException.invalid("Situação não permitida para este modelo de relatório");
    }
    private long parse(List<String> row,int index) { return Long.parseLong(row.get(index)); }
}
