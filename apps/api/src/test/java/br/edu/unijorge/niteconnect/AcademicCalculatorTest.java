package br.edu.unijorge.niteconnect;

import java.math.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import br.edu.unijorge.niteconnect.academic.domain.*;
import br.edu.unijorge.niteconnect.academic.domain.AcademicRecords.*;
import br.edu.unijorge.niteconnect.academic.domain.AcademicStates.Pillar;
import static org.assertj.core.api.Assertions.*;

class AcademicCalculatorTest {
    PolicyRules rules() { return new PolicyRules(Map.of(Pillar.DOCUMENTATION,new BigDecimal("30"),Pillar.PRESENTATIONS,new BigDecimal("20"),Pillar.DELIVERIES,new BigDecimal("50")),
            BigDecimal.ZERO,BigDecimal.TEN,2,RoundingMode.HALF_UP,List.of(new Discipline("TEST","TEST")),2,Instant.parse("2026-01-01T00:00:00Z"),Instant.parse("2027-01-01T00:00:00Z"),null,null,null,null); }
    Assessment input(Pillar p,String score) { return new Assessment(UUID.randomUUID(),UUID.randomUUID(),p,new BigDecimal(score),List.of(UUID.randomUUID()),"Teste",1,UUID.randomUUID(),Instant.now()); }
    @Test void weightedDecimalResultIsExactAndZeroIsNotMissing() {
        assertThat(AcademicCalculator.calculate(rules(),List.of(input(Pillar.DOCUMENTATION,"8"),input(Pillar.PRESENTATIONS,"7"),input(Pillar.DELIVERIES,"9")))).isEqualByComparingTo("8.30");
        assertThat(AcademicCalculator.calculate(rules(),List.of(input(Pillar.DOCUMENTATION,"0"),input(Pillar.PRESENTATIONS,"0"),input(Pillar.DELIVERIES,"0")))).isEqualByComparingTo("0.00");
        assertThatThrownBy(()->AcademicCalculator.calculate(rules(),List.of(input(Pillar.DOCUMENTATION,"0")))).isInstanceOf(br.edu.unijorge.niteconnect.shared.api.ApiException.class);
    }
    @Test void duplicatePillarsOutOfRangeAndHugeDecimalsAreRejected() {
        assertThatThrownBy(()->AcademicCalculator.calculate(rules(),List.of(input(Pillar.DOCUMENTATION,"8"),input(Pillar.DOCUMENTATION,"7"),input(Pillar.DELIVERIES,"9")))).isInstanceOf(br.edu.unijorge.niteconnect.shared.api.ApiException.class);
        assertThatThrownBy(()->AcademicCalculator.score(rules(),new BigDecimal("10.01"))).isInstanceOf(br.edu.unijorge.niteconnect.shared.api.ApiException.class);
        assertThatThrownBy(()->AcademicCalculator.score(rules(),new BigDecimal("1E-10000"))).isInstanceOf(br.edu.unijorge.niteconnect.shared.api.ApiException.class);
    }
    @Test void scoreBoundsMustBeRepresentableAtTheOfficialResultScale() {
        PolicyRules r=rules();
        var incompatible=new PolicyRules(r.weights(),r.minimumScore(),new BigDecimal("9.999"),r.scale(),r.rounding(),r.eligibleDisciplines(),r.maximumDisciplines(),r.enrollmentOpensAt(),r.enrollmentClosesAt(),null,null,null,null);
        assertThatThrownBy(()->AcademicRules.policy(incompatible)).isInstanceOf(br.edu.unijorge.niteconnect.shared.api.ApiException.class);
    }
}
