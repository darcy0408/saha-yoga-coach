package io.saha.yoga.routine;

import org.junit.jupiter.api.Test;

import java.lang.LazyConstant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class Java26LazyConstantTest {
    @Test void initializesExactlyOnceOnFirstAccess() {
        var calls = new AtomicInteger();
        var value = LazyConstant.of(() -> {
            calls.incrementAndGet();
            return "pose catalog";
        });

        assertFalse(value.isInitialized());
        assertEquals("pose catalog", value.get());
        assertEquals("pose catalog", value.get());
        assertTrue(value.isInitialized());
        assertEquals(1, calls.get());
    }
}
