package io.saha.yoga.storage;

import io.saha.yoga.domain.SessionMetric;
import java.io.IOException;
import java.util.List;

public interface SessionStore {
    List<SessionMetric> load() throws IOException;
    void append(SessionMetric metric) throws IOException;
    void deleteAll() throws IOException;
}

