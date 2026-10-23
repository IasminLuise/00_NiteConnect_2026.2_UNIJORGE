package br.edu.unijorge.niteconnect.academic.domain;

import java.math.BigDecimal;
import java.util.*;
import br.edu.unijorge.niteconnect.academic.domain.AcademicRecords.*;
import br.edu.unijorge.niteconnect.academic.domain.AcademicStates.Pillar;
import br.edu.unijorge.niteconnect.shared.api.ApiException;

public final class AcademicCalculator {
    public static final String ALGORITHM="weighted-decimal-v1";
    private AcademicCalculator() {}
    public static BigDecimal calculate(PolicyRules policy,List<Assessment> inputs) {
        AcademicRules.policy(policy);
        if(inputs==null || inputs.size()!=Pillar.values().length) throw ApiException.conflict("Avaliação incompleta: faltam pilares");
        Set<Pillar> seen=EnumSet.noneOf(Pillar.class);BigDecimal total=BigDecimal.ZERO;
        for(var a:inputs) {
            if(a==null || a.pillar()==null || !seen.add(a.pillar())) throw ApiException.invalid("Pilares duplicados ou inválidos");
            score(policy,a.score());total=total.add(a.score().multiply(policy.weights().get(a.pillar())));
        }
        return total.movePointLeft(2).setScale(policy.scale(),policy.rounding());
    }
    public static void score(PolicyRules p,BigDecimal score) {
        if(score==null || score.precision()>20 || score.scale()>8 || score.scale()< -8 || score.compareTo(p.minimumScore())<0 || score.compareTo(p.maximumScore())>0) throw ApiException.invalid("Nota fora da escala oficial");
    }
}
