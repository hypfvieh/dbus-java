package org.freedesktop.dbus.connections;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.freedesktop.dbus.connections.ReceivingService.ExecutorNames;
import org.freedesktop.dbus.connections.ReceivingService.IThreadPoolRetryHandler;
import org.freedesktop.dbus.connections.config.ReceivingServiceConfig;
import org.freedesktop.dbus.connections.config.ReceivingServiceConfigBuilder;
import org.freedesktop.dbus.exceptions.IllegalThreadPoolStateException;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;

class ReceivingServiceTest extends AbstractBaseTest {

    /**
     * Tests that no retry is attempted when no retry handler is installed.
     */
    @Test
    void testRetryHandlerNotUsed() {

        ReceivingServiceConfig build = new ReceivingServiceConfigBuilder<>(null).withRetryHandler(null).build();

        ReceivingService service = new ReceivingService(build) {
            ExecutorService getExecutor(ExecutorNames _executor) {
                return new NoOpExecutorService();
            }
        };

        int fails = service.execOrFail(ExecutorNames.SIGNAL, () -> System.out.println("hi"));

        assertEquals(0, fails, "No retry attempt expected");
    }

    /**
     * Tests that 5 retries will be attempted due to the handler returning true 5 times.
     */
    @Test
    void testRetryHandlerCalled5Times() {
        IThreadPoolRetryHandler handler = new IThreadPoolRetryHandler() {
            private int count = 0;
            @Override
            public boolean handle(ExecutorNames _executor, Exception _ex) {
                count++;
                if (count < 5) {
                    return true;
                } else {
                    return false;
                }
            }
        };

        ReceivingServiceConfig build = new ReceivingServiceConfigBuilder<>(null).withRetryHandler(handler).build();

        ReceivingService service = new ReceivingService(build) {
            ExecutorService getExecutor(ExecutorNames _executor) {
                return new NoOpExecutorService();
            }
        };

        int fails = service.execSignalHandler(() -> System.out.println("hi"));

        assertEquals(5, fails, "5 retry attempts expected");
    }

    /**
     * Test that the default retry handler is only called for the defined retries.
     */
    @Test
    void testDefaultRetryHandler() {
        ReceivingServiceConfig build = new ReceivingServiceConfigBuilder<>(null).build();

        ReceivingService service = new ReceivingService(build) {
            ExecutorService getExecutor(ExecutorNames _executor) {
                return new NoOpExecutorService();
            }
        };

        int fails = service.execMethodCallHandler(() -> System.out.println("hi"));

        assertEquals(ReceivingServiceConfigBuilder.DEFAULT_HANDLER_RETRIES, fails, ReceivingServiceConfigBuilder.DEFAULT_HANDLER_RETRIES + " retry attempts expected");
    }

    /**
     * Test that retrying will be interrupted when the hard limit of {@value ReceivingService#MAX_RETRIES} is reached.
     */
    @Test
    void testRetryHandlerHardLimit() {
        IThreadPoolRetryHandler handler = new IThreadPoolRetryHandler() {
            @Override
            public boolean handle(ExecutorNames _executor, Exception _ex) {
                return true;
            }
        };

        ReceivingServiceConfig build = new ReceivingServiceConfigBuilder<>(null).withRetryHandler(handler).build();

        ReceivingService service = new ReceivingService(build) {
            ExecutorService getExecutor(ExecutorNames _executor) {
                return new NoOpExecutorService();
            }
        };

        int fails = service.execMethodReturnHandler(() -> System.out.println("hi"));

        assertEquals(ReceivingService.MAX_RETRIES, fails, ReceivingService.MAX_RETRIES + " retry attempts expected");
    }

    /**
     * Test that verifies -1 is returned when any null parameter is given.
     */
    @Test
    void testExecuteAnyNull() {
        ReceivingServiceConfig build = new ReceivingServiceConfigBuilder<>(null).withRetryHandler(null).build();

        ReceivingService service = new ReceivingService(build) {
            ExecutorService getExecutor(ExecutorNames _executor) {
                return new NoOpExecutorService();
            }
        };

        int noExecName = service.execOrFail(null, () -> System.out.println("hi"));
        int noRunnable = service.execOrFail(ExecutorNames.METHODCALL, null);
        int nonOfAll = service.execOrFail(null, null);

        assertEquals(-1, noExecName, "-1 retries expected");
        assertEquals(-1, noRunnable, "-1 retries expected");
        assertEquals(-1, nonOfAll, "-1 retries expected");
    }

    /**
     * Test that checks that the retry handler has not been called because no exception was thrown.
     */
    @Test
    void testRetryHandlerNotCalledBecauseNoFailure() {
        AtomicBoolean handlerWasCalled = new AtomicBoolean();
        IThreadPoolRetryHandler handler = new IThreadPoolRetryHandler() {

            @Override
            public boolean handle(ExecutorNames _executor, Exception _ex) {
                handlerWasCalled.set(true);
                return true;
            }
        };

        ReceivingServiceConfig build = new ReceivingServiceConfigBuilder<>(null).withRetryHandler(handler).build();

        ReceivingService service = new ReceivingService(build) {
            ExecutorService getExecutor(ExecutorNames _executor) {
                NoOpExecutorService noOpExecutorService = new NoOpExecutorService();
                noOpExecutorService.throwException = false;
                return noOpExecutorService;
            }
        };

        int fails = service.execErrorHandler(() -> System.out.println("hi"));

        assertEquals(0, fails, "0 retry attempts expected");
        assertFalse(handlerWasCalled.get(), "Handler should not have been called");
    }

    /**
     * Test that a proper exception is thrown when no executor was returned.
     */
    @Test
    void testExecutorNull() {
        ReceivingServiceConfig build = new ReceivingServiceConfigBuilder<>(null).withRetryHandler(null).build();

        ReceivingService service = new ReceivingService(build) {
            ExecutorService getExecutor(ExecutorNames _executor) {
                return null;
            }
        };

        IllegalThreadPoolStateException ex = assertThrows(IllegalThreadPoolStateException.class, () -> service.execOrFail(ExecutorNames.SIGNAL, () -> System.out.println("hi")));
        assertEquals("No executor found for " + ExecutorNames.SIGNAL, ex.getMessage());
    }

    /**
     * Test that a proper exception is thrown when executor was shutdown or terminated.
     */
    @Test
    void testExecutorShutdownOrTerminated() {
        ReceivingServiceConfig build = new ReceivingServiceConfigBuilder<>(null).withRetryHandler(null).build();
        var exec = new NoOpExecutorService();
        exec.shutdown = true;

        ReceivingService service = new ReceivingService(build) {
            ExecutorService getExecutor(ExecutorNames _executor) {
                return exec;
            }
        };

        IllegalThreadPoolStateException ex = assertThrows(IllegalThreadPoolStateException.class, () -> service.execOrFail(ExecutorNames.SIGNAL, () -> System.out.println("hi")));
        assertEquals("Receiving service already closed", ex.getMessage());

        exec.shutdown = false;
        exec.terminated = true;

        IllegalThreadPoolStateException ex2 = assertThrows(IllegalThreadPoolStateException.class, () -> service.execOrFail(ExecutorNames.SIGNAL, () -> System.out.println("hi")));
        assertEquals("Receiving service already closed", ex2.getMessage());
    }

    /**
     * Test that a proper exception is thrown when service was closed.
     */
    @Test
    void testReceivingServiceClosed() {
        ReceivingServiceConfig build = new ReceivingServiceConfigBuilder<>(null).withRetryHandler(null).build();

        ReceivingService service = new ReceivingService(build) {
            ExecutorService getExecutor(ExecutorNames _executor) {
                return new NoOpExecutorService();
            }
        };

        service.shutdownNow();

        IllegalThreadPoolStateException ex = assertThrows(IllegalThreadPoolStateException.class, () -> service.execOrFail(ExecutorNames.SIGNAL, () -> System.out.println("hi")));
        assertEquals("Receiving service already closed", ex.getMessage());

    }

    /**
     * Executor service which does nothing but throw exceptions.
     */
    class NoOpExecutorService implements ExecutorService {

        protected boolean throwException = true;
        boolean shutdown;
        boolean terminated;

        @Override
        public void execute(Runnable _command) {
            if (throwException) {
                throw new NullPointerException("This executor is broken");
            }
        }

        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return null;
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return terminated;
        }

        @Override
        public boolean awaitTermination(long _timeout, TimeUnit _unit) throws InterruptedException {
            return false;
        }

        @Override
        public <T> Future<T> submit(Callable<T> _task) {
            return null;
        }

        @Override
        public <T> Future<T> submit(Runnable _task, T _result) {
            return null;
        }

        @Override
        public Future<?> submit(Runnable _task) {
            return null;
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> _tasks) throws InterruptedException {
            return null;
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> _tasks, long _timeout, TimeUnit _unit)
                throws InterruptedException {
            return null;
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> _tasks)
                throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> _tasks, long _timeout, TimeUnit _unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }

    }
}
