package io.saha.yoga;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * How solidly a joint is drawn, from its confidence.
 *
 * The drawing rule lives in the application class, so it is reached
 * reflectively rather than copied here, where the copy could drift from the
 * thing being checked.
 */
class VisibilityRampTest {
    private double visibility(double confidence) throws Exception {
        Method method = Class.forName("io.saha.yoga.SahaApp").getDeclaredMethod("visibility", double.class);
        method.setAccessible(true);
        return (double) method.invoke(null, confidence);
    }

    @Test void aJointInPlainViewIsDrawnSolid() throws Exception {
        assertEquals(1, visibility(.9), 1e-9);
        assertEquals(1, visibility(.30), 1e-9, "the drawing threshold itself is still fully solid");
    }

    @Test void aJointTheModelHasNoOpinionAboutIsNotDrawn() throws Exception {
        assertEquals(0, visibility(.08), 1e-9);
        assertEquals(0, visibility(.01), 1e-9);
    }

    @Test void aHiddenJointIsDrawnFaintlyRatherThanDropped() throws Exception {
        // sitting cross-legged scores knees and ankles here however well you sit
        double faint = visibility(.15);
        assertTrue(faint > 0 && faint < .5, "a hidden leg should be present but clearly uncertain: " + faint);
    }

    @Test void thereIsNoStepAtTheThreshold() throws Exception {
        // a step is what made an earlier attempt flicker: a joint hovering at
        // the line flipped between solid and faint several times a second
        double justUnder = visibility(.299), atLine = visibility(.30);
        assertTrue(atLine - justUnder < .05,
                "crossing the threshold should be imperceptible, jumped by " + (atLine - justUnder));
    }

    @Test void theRampRisesWithConfidence() throws Exception {
        double previous = -1;
        for (double confidence = .05; confidence <= .35; confidence += .01) {
            double current = visibility(confidence);
            assertTrue(current >= previous, "visibility must never fall as confidence rises, at " + confidence);
            previous = current;
        }
    }
}
