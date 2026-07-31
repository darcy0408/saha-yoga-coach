package io.saha.yoga.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.saha.yoga.domain.SessionMetric;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class JsonSessionStore implements SessionStore {
    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    public JsonSessionStore(Path file) { this.file = file; }
    @Override public synchronized List<SessionMetric> load() throws IOException {
        if (!Files.exists(file)) return List.of();
        return List.copyOf(mapper.readValue(file.toFile(), new TypeReference<List<SessionMetric>>() {}));
    }
    @Override public synchronized void append(SessionMetric metric) throws IOException {
        var all = new ArrayList<>(load()); all.add(metric);
        Files.createDirectories(file.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), all);
    }
    @Override public synchronized void deleteAll() throws IOException { Files.deleteIfExists(file); }
}
