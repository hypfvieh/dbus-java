package org.freedesktop.dbus.utils;

import org.freedesktop.dbus.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;

public class TimeMeasureTest extends AbstractBaseTest {

    @Test
    public void testTimeMeasure() {
        TimeMeasure tm = new TimeMeasure();
        assertTrue(tm.getElapsed() >= 0);
        try {
            Thread.sleep(100L);
        } catch (InterruptedException _ex) {
            assertTrue(true);
        }
        assertTrue(tm.getElapsed() >= 100);
        assertTrue(tm.toString().matches("^[0-9]+ms$"), "toString() returned " + tm);

        tm.reset();
        long elapsed = tm.getElapsed();
        assertTrue(elapsed < 10);

        tm.setStartTm(Duration.ofMillis(Duration.ofNanos(tm.getStartTime()).toMillis() - 10000).toNanos());
        elapsed = tm.getElapsed();
        assertTrue(elapsed >= 10000, "Elapsed was " + elapsed);
        String toStringResult = tm.toString();
        assertTrue(toStringResult.matches("^[0-9]+\\.[0-9]+s$"), "toString() returned " + toStringResult);
    }

    @Test
    public void testTimeMeasureFormatter() {
        TimeMeasure tm = new TimeMeasure();

        String oneSecond = tm.getElapsedFormatted(null, 1000);
        assertEquals("00:00:01.000", oneSecond);

        String oneMinuteoneSecond = tm.getElapsedFormatted(null, 61000);
        assertEquals("00:01:01.000", oneMinuteoneSecond);

        String threeSecondsAfewMillis = tm.getElapsedFormatted(null, 3721);
        assertEquals("00:00:03.721", threeSecondsAfewMillis);
    }
}
