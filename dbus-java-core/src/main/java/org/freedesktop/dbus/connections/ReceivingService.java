package org.freedesktop.dbus.connections;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.freedesktop.dbus.utils.NameableThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service providing threads for every type of message expected to be received by DBus.
 * 
 * @author hypfvieh
 * @version 4.1.0 - 2022-02-02
 */
public class ReceivingService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private boolean closed = false;
    
    private final Map<ExecutorNames, ExecutorService> executors = new EnumMap<>(ExecutorNames.class);
    
    public ReceivingService() {
        executors.put(ExecutorNames.SIGNAL, Executors.newSingleThreadExecutor(new NameableThreadFactory("DBus-Signal-Receiver-", true)));
        executors.put(ExecutorNames.ERROR, Executors.newSingleThreadExecutor(new NameableThreadFactory("DBus-Error-Receiver-", true)));

        // we need multiple threads here so recursive method calls are possible
        executors.put(ExecutorNames.METHODCALL, Executors.newFixedThreadPool(4, new NameableThreadFactory("DBus-MethodCall-Receiver-", true)));
        executors.put(ExecutorNames.METHODRETURN, Executors.newSingleThreadExecutor(new NameableThreadFactory("DBus-MethodReturn-Receiver-", true)));
    }
    
    /**
     * Execute a runnable which handles a signal.
     * @param _r runnable
     */
    void execSignalHandler(Runnable _r) {
        execOrFail(ExecutorNames.SIGNAL, _r);
    }

    /**
     * Execute a runnable which handles an error.
     * @param _r runnable
     */
    void execErrorHandler(Runnable _r) {
        execOrFail(ExecutorNames.ERROR, _r);
    }

    /**
     * Execute a runnable which handles a method call.
     * @param _r runnable
     */
    void execMethodCallHandler(Runnable _r) {
        execOrFail(ExecutorNames.METHODCALL, _r);
    }

    /**
     * Execute a runnable which handles the return of a method.
     * @param _r runnable
     */
    void execMethodReturnHandler(Runnable _r) {
        execOrFail(ExecutorNames.METHODRETURN, _r);
    }
    
    /**
     * Executes a runnable in a given executor.
     * @param _executor executor to use
     * @param _r runnable
     */
    void execOrFail(ExecutorNames _executor, Runnable _r) {
        if (_r == null || _executor == null) { // ignore invalid runnables or executors
            return;
        }
        ExecutorService exec = executors.get(_executor);
        if (closed || exec.isShutdown() || exec.isTerminated()) {
            throw new IllegalStateException("Receiving service already closed");
        }
        exec.execute(_r);
    }
    
    /**
     * Shutdown all executor services waiting up to the given timeout/unit.
     *  
     * @param _timeout timeout
     * @param _unit time unit
     * @throws InterruptedException when interrupted while waiting
     */
    public synchronized void shutdown(int _timeout, TimeUnit _unit) {
        for (Entry<ExecutorNames, ExecutorService> es : executors.entrySet()) {
            logger.debug("Shutting down executor: {}", es.getKey());
            es.getValue().shutdown();
        }

        for (Entry<ExecutorNames, ExecutorService> es : executors.entrySet()) {
            try {
                es.getValue().awaitTermination(_timeout, _unit);
            } catch (InterruptedException _ex) {
                LoggerFactory.getLogger(getClass()).warn("Interrupted while waiting for termination of executor", _ex);
            }
        }

        closed = true;
    }

    /**
     * Forcefully stop the executors.
     */
    public synchronized void shutdownNow() {
        for (Entry<ExecutorNames, ExecutorService> es : executors.entrySet()) {
            if (!es.getValue().isTerminated()) {
                logger.debug("Forcefully stopping {}", es.getKey());
                es.getValue().shutdownNow();
            }
        }
    }
    
    /**
     * Enum representing different executor services.
     * 
     * @author hypfvieh
     * @version 4.0.1 - 2022-02-02
     */
    enum ExecutorNames {
        SIGNAL("SignalExecutor"),
        ERROR("ErrorExecutor"),
        METHODCALL("MethodCallExecutor"),
        METHODRETURN("MethodReturnExecutor");
        
        private final String description;
        
        ExecutorNames(String _name) {
            description = _name;
        }

        public String getDescription() {
            return description;
        }
        
        @Override
        public String toString() {
            return description;
        }
    }
}
