package io.saha.yoga.speech;

/** Somewhere to send a spoken line. Implementations must never block the caller. */
public interface Voice extends AutoCloseable {
    void say(String text);

    /** True when speech is actually available; the interface stays usable either way. */
    boolean isAvailable();

    @Override void close();

    /** Used when the platform has no voice, or the user turned speech off. */
    static Voice silent() {
        return new Voice() {
            @Override public void say(String text) { }
            @Override public boolean isAvailable() { return false; }
            @Override public void close() { }
        };
    }
}
