package br.edu.unijorge.niteconnect.academic.domain;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import br.edu.unijorge.niteconnect.academic.domain.AcademicRecords.*;
import br.edu.unijorge.niteconnect.academic.domain.AcademicStates.*;
import br.edu.unijorge.niteconnect.shared.api.ApiException;

public final class AcademicRules {
    private AcademicRules() {}
    public static void text(String value, int max, String label) {
        if(value==null || value.isBlank() || value.length()>max) throw ApiException.invalid(label+" inválido");
    }
    public static void reason(String value) { text(value,1000,"Motivo"); }
    public static void version(long actual,Long expected) {
        if(expected==null) throw ApiException.invalid("Versão esperada obrigatória");
        if(expected<0 || actual!=expected) throw ApiException.conflict("Registro alterado; consulte a versão atual");
    }
    public static void confirmed(boolean value) { if(!value) throw ApiException.invalid("Confirmação obrigatória"); }
    public static void term(TermData t) {
        text(t.title(),100,"Título"); text(t.zone(),80,"Fuso");
        interval(t.startsAt(),t.endsAt());
        try { ZoneId.of(t.zone()); } catch(DateTimeException ex) { throw ApiException.invalid("Fuso inválido"); }
    }
    public static void interval(Instant from,Instant to) {
        Instant lower=Instant.parse("0001-01-01T00:00:00Z"),upper=Instant.parse("9999-12-31T23:59:59Z");
        if(from==null || to==null || !from.isBefore(to) || from.isBefore(lower) || to.isAfter(upper)) throw ApiException.invalid("Intervalo inválido");
    }
    public static Discipline discipline(Discipline d) {
        if(d==null) throw ApiException.invalid("Disciplina inválida");
        text(d.courseCode(),80,"Curso");text(d.code(),80,"Disciplina");
        return new Discipline(d.courseCode().strip().toUpperCase(Locale.ROOT),d.code().strip().toUpperCase(Locale.ROOT));
    }
    public static List<Discipline> disciplines(List<Discipline> ds) {
        if(ds==null || ds.isEmpty() || ds.size()>100) throw ApiException.invalid("Disciplinas inválidas");
        var result=ds.stream().map(AcademicRules::discipline).toList();
        if(new HashSet<>(result).size()!=result.size()) throw ApiException.invalid("Disciplina duplicada");
        return result;
    }
    public static void policy(PolicyRules r) {
        if(r==null || r.weights()==null || r.weights().size()!=Pillar.values().length ||
           r.minimumScore()==null || r.maximumScore()==null || r.minimumScore().precision()>20 || r.maximumScore().precision()>20 || Math.abs((long)r.minimumScore().scale())>8 || Math.abs((long)r.maximumScore().scale())>8 || r.minimumScore().signum()<0 || r.minimumScore().compareTo(r.maximumScore())>=0 ||
           r.scale()==null || r.scale()<0 || r.scale()>8 || r.rounding()==null || r.rounding()==java.math.RoundingMode.UNNECESSARY)
            throw ApiException.invalid("Escala, pesos e arredondamento oficiais são obrigatórios");
        BigDecimal sum=BigDecimal.ZERO;
        for(Pillar p:Pillar.values()) { BigDecimal w=r.weights().get(p); if(w==null || w.signum()<0 || Math.abs((long)w.scale())>8 || w.precision()>20) throw ApiException.invalid("Peso inválido"); sum=sum.add(w); }
        if(sum.compareTo(new BigDecimal("100"))!=0) throw ApiException.invalid("Pesos devem somar 100%");
        try {
            r.minimumScore().setScale(r.scale(),java.math.RoundingMode.UNNECESSARY);
            r.maximumScore().setScale(r.scale(),java.math.RoundingMode.UNNECESSARY);
        } catch(ArithmeticException ex) { throw ApiException.invalid("Escala não representa os limites oficiais"); }
        if(r.maximumDisciplines()==null || r.maximumDisciplines()<1 || r.maximumDisciplines()>100) throw ApiException.invalid("Limite institucional obrigatório");
        disciplines(r.eligibleDisciplines()); interval(r.enrollmentOpensAt(),r.enrollmentClosesAt());
        if(r.reviewWindowSeconds()!=null && (r.reviewWindowSeconds()<1 || r.reviewWindowSeconds()>315576000L)) throw ApiException.invalid("Prazo de revisão inválido");
        if(r.forwardingProcedureReference()!=null) text(r.forwardingProcedureReference(),1000,"Procedimento");
    }
    public static void enrollment(Policy p,List<Discipline> ds,Instant now) {
        if(!p.status().equals("CONFIRMED")) throw ApiException.conflict("Política ainda não confirmada");
        var r=p.rules();
        if(now.isBefore(r.enrollmentOpensAt()) || !now.isBefore(r.enrollmentClosesAt())) throw ApiException.conflict("Fora da janela de adesão");
        if(ds.size()>r.maximumDisciplines()) throw ApiException.invalid("Limite de disciplinas excedido");
        var eligible=r.eligibleDisciplines().stream().map(AcademicRules::discipline).toList();
        if(!eligible.containsAll(ds)) throw ApiException.invalid("Curso ou disciplina não elegível");
    }
}
