package org.freedesktop.dbus.test;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;

import org.freedesktop.dbus.bin.EmbeddedDBusDaemon;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base test class providing logger and common methods.
 *
 * @author hypfvieh
 * @since v4.0.0 - 2021-09-14
 */
public class AbstractBaseTest extends Assertions {
    /** Max wait time to wait for daemon to start. */
    private static final long MAX_WAIT = Duration.ofSeconds(30).toMillis();

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /** Holds information about the current test. */
    private TestInfo lastTestInfo;

    @BeforeEach
    public final void setTestMethodName(TestInfo _testInfo) {
        lastTestInfo = _testInfo;
    }

    protected final String getTestMethodName() {
        if (lastTestInfo != null && lastTestInfo.getTestClass().isPresent()) {
            return lastTestInfo.getTestClass().get().getName() + '.' + lastTestInfo.getTestMethod().get().getName();
        }
        return null;
    }

    protected final String getShortTestMethodName() {
        Optional<Method> testMethod = lastTestInfo == null ? Optional.empty() : lastTestInfo.getTestMethod();
        return testMethod.map(Method::getName).orElse(null);
    }

    @BeforeEach
    public final void logTestBegin(TestInfo _testInfo) {
        logTestBeginEnd("BGN", _testInfo);
    }

    @AfterEach
    public final void logTestEnd(TestInfo _testInfo) {
        logTestBeginEnd("END", _testInfo);
    }

    protected void logTestBeginEnd(String _prefix, TestInfo _testInfo) {
        if (!_testInfo.getTestMethod().isPresent() || _testInfo.getDisplayName().startsWith(_testInfo.getTestMethod().get().getName())) {
            logger.info(">>>>>>>>>> {} Test: {} <<<<<<<<<<", _prefix, _testInfo.getDisplayName());
        } else {
            logger.info(">>>>>>>>>> {} Test: {} ({}) <<<<<<<<<<", _prefix, _testInfo.getTestMethod().get().getName(), _testInfo.getDisplayName());
        }
    }

    protected void waitForDaemon(EmbeddedDBusDaemon daemon) {
        long sleepMs = 500;
        long waited = 0;

        do {
            if (waited >= MAX_WAIT) {
                throw new RuntimeException("EmbeddedDbusDaemon not started in the specified time of " + MAX_WAIT + " ms");
            }

            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException _ex) {
                break;
            }

            waited += sleepMs;
            logger.debug("Waiting for embedded daemon to start: {} of {} ms waited", waited, MAX_WAIT);
        } while (!daemon.isRunning());
    }
}
