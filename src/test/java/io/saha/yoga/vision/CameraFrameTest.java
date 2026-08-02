package io.saha.yoga.vision;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CameraFrameTest {
    @Test void validatesPayloadSize() {
        assertThrows(IllegalArgumentException.class, () -> new CameraFrame(2, 2, new byte[15]));
        assertThrows(IllegalArgumentException.class, () -> new CameraFrame(0, 2, new byte[0]));
    }

    @Test void protectsTransientPixelsFromMutation() {
        var source = new byte[16];
        var frame = new CameraFrame(2, 2, source);
        source[0] = 9;
        assertEquals(0, frame.bgra()[0]);
        var returned = frame.bgra();
        returned[1] = 7;
        assertEquals(0, frame.bgra()[1]);
    }
}
