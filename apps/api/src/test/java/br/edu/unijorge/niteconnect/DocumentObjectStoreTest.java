package br.edu.unijorge.niteconnect;

import br.edu.unijorge.niteconnect.documents.infrastructure.LocalDocumentObjectStore;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;

class DocumentObjectStoreTest {
    @TempDir Path directory;

    @Test void disabledWithoutExplicitPrivateDevelopmentRoot() {
        var store = new LocalDocumentObjectStore("", true, new org.springframework.mock.env.MockEnvironment());
        assertThat(store.enabled()).isFalse();
        assertThatThrownBy(() -> store.put(new byte[]{1})).isInstanceOf(ApiException.class);
        assertThat(new LocalDocumentObjectStore(directory.toString(), false, new org.springframework.mock.env.MockEnvironment()).enabled()).isFalse();
        assertThat(new LocalDocumentObjectStore(directory.toString(), true,
                new org.springframework.mock.env.MockEnvironment().withProperty("spring.profiles.active", "production")).enabled()).isFalse();
    }

    @Test void roundTripsSeparateImmutableObjectsAndChecksRecordedIntegrity() throws Exception {
        var store = new LocalDocumentObjectStore(directory.toString(), true, new org.springframework.mock.env.MockEnvironment());
        byte[] content = {1, 2, 3};
        var first = store.put(content);
        var second = store.put(content);
        assertThat(first.key()).isNotEqualTo(second.key());
        assertThat(store.read(first.key(), first.size(), first.sha256())).isEqualTo(content);
        assertThatThrownBy(() -> store.read(first.key(), 2, first.sha256())).isInstanceOf(ApiException.class);
        Files.write(directory.resolve(first.key().toString()), new byte[]{3, 2, 1});
        assertThatThrownBy(() -> store.read(first.key(), first.size(), first.sha256())).isInstanceOf(ApiException.class);
        assertThat(Files.exists(directory.resolve(second.key().toString()))).isTrue();
    }

    @Test void rejectsEmptyOversizedAndMissingObjects() {
        var store = new LocalDocumentObjectStore(directory.toString(), true, new org.springframework.mock.env.MockEnvironment());
        assertThatThrownBy(() -> store.put(new byte[0])).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> store.put(new byte[8 * 1024 * 1024 + 1])).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> store.read(java.util.UUID.randomUUID(), 1, "invalid")).isInstanceOf(ApiException.class);
    }

    @Test void rejectsSymbolicLinkRootAncestorAndObjectWhenSupported() throws Exception {
        Path real = Files.createDirectory(directory.resolve("real"));
        Path link = directory.resolve("linked");
        try { Files.createSymbolicLink(link, real); }
        catch (UnsupportedOperationException | java.io.IOException e) {
            org.junit.jupiter.api.Assumptions.abort("Symbolic links unavailable on this host");
        }
        assertThatThrownBy(() -> new LocalDocumentObjectStore(link.toString(), true, new org.springframework.mock.env.MockEnvironment()).put(new byte[]{1})).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> new LocalDocumentObjectStore(link.resolve("child").toString(), true, new org.springframework.mock.env.MockEnvironment()).put(new byte[]{1})).isInstanceOf(ApiException.class);
        var store = new LocalDocumentObjectStore(real.toString(), true, new org.springframework.mock.env.MockEnvironment());
        var stored = store.put(new byte[]{1});
        Path object = real.resolve(stored.key().toString());
        Files.delete(object);
        Files.createSymbolicLink(object, Files.write(directory.resolve("outside"), new byte[]{1}));
        assertThatThrownBy(() -> store.read(stored.key(), stored.size(), stored.sha256())).isInstanceOf(ApiException.class);
    }
}
