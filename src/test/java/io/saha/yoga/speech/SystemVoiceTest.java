package io.saha.yoga.speech;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class SystemVoiceTest {
    private boolean onWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    @Test void aVoiceStartsOnThisPlatformOrFallsBackQuietly() {
        var voice = SystemVoice.create();
        try {
            // deliberately says nothing: the point is that the helper process
            // starts and stays up, not that the room hears a test
            if (onWindows()) assertTrue(voice.isAvailable(), "Windows should provide a speech helper");
            else assertFalse(voice.isAvailable(), "an unknown platform must degrade to silence, not crash");
        } finally {
            voice.close();
        }
    }

    @Test void theSilentVoiceAcceptsEverythingAndSaysNothing() {
        var voice = Voice.silent();
        voice.say("this goes nowhere");
        assertFalse(voice.isAvailable());
        voice.close();
    }

    @Test void closingTwiceIsHarmless() {
        var voice = SystemVoice.create();
        voice.close();
        assertDoesNotThrow(voice::close);
    }
}
