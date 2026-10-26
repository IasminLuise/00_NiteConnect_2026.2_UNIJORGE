package br.edu.unijorge.niteconnect;

import br.edu.unijorge.niteconnect.access.domain.GlobalRole;
import br.edu.unijorge.niteconnect.diaries.application.DiaryAccessPolicy;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class DiaryAccessPolicyTest {
    private final DiaryAccessPolicy policy = new DiaryAccessPolicy();
    private final UUID author = UUID.randomUUID();
    private final UUID administrator = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();

    @Test void authorCanReadAndEditTheirDiary() {
        assertThatCode(() -> policy.requireRead(author, author, List.of())).doesNotThrowAnyException();
        assertThatCode(() -> policy.requireEdit(author, author)).doesNotThrowAnyException();
    }

    @Test void coordinatorAndGeneralManagerCanReadButCannotEdit() {
        for (GlobalRole role : List.of(GlobalRole.COORDENADOR, GlobalRole.GESTOR_GERAL)) {
            assertThatCode(() -> policy.requireRead(administrator, author, List.of(role))).doesNotThrowAnyException();
            assertThatThrownBy(() -> policy.requireEdit(administrator, author)).isInstanceOf(ApiException.class);
        }
    }

    @Test void projectManagerAndUnrelatedMemberCannotReadAnotherDiary() {
        assertThatThrownBy(() -> policy.requireRead(other, author, List.of())).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> policy.requireEdit(other, author)).isInstanceOf(ApiException.class);
    }

    @Test void onlyCoordinatorOrGeneralManagerMayImportForAnotherMember() {
        assertThatCode(() -> policy.requireImport(administrator, List.of(GlobalRole.COORDENADOR))).doesNotThrowAnyException();
        assertThatCode(() -> policy.requireImport(administrator, List.of(GlobalRole.GESTOR_GERAL))).doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.requireImport(other, List.of())).isInstanceOf(ApiException.class);
    }
}
