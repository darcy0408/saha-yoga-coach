package io.saha.yoga.storage;

import io.saha.yoga.domain.SessionMetric;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class JsonSessionStoreTest {
    @TempDir Path temp;
    @Test void roundTripsAndDeletesDerivedMetric() throws Exception {
        var store = new JsonSessionStore(temp.resolve("sessions.json"));
        store.append(new SessionMetric("tree", Instant.parse("2026-07-31T00:00:00Z"), 30, .8, 1, false, .9, true));
        assertEquals(1, store.load().size()); store.deleteAll(); assertTrue(store.load().isEmpty());
    }
}
