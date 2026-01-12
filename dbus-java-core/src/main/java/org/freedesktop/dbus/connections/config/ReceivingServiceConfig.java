package org.freedesktop.dbus.connections.config;

import org.freedesktop.dbus.connections.shared.ExecutorNames;
import org.freedesktop.dbus.connections.shared.IThreadPoolRetryHandler;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Bean which holds configuration for {@link org.freedesktop.dbus.connections.base.ReceivingService}.
 *
 * @author hypfvieh
 * @since 4.2.0 - 2022-07-14
 */
public final class ReceivingServiceConfig {

    private final Map<ExecutorNames, ThreadCfg> threadConfigs = new EnumMap<>(ExecutorNames.class);

    private IThreadPoolRetryHandler retryHandler;

    ReceivingServiceConfig() {
        threadConfigs.put(ExecutorNames.SIGNAL, new ThreadCfg(1, Thread.NORM_PRIORITY, false));
        threadConfigs.put(ExecutorNames.ERROR, new ThreadCfg(1, Thread.NORM_PRIORITY, false));
        threadConfigs.put(ExecutorNames.METHODCALL, new ThreadCfg(4, Thread.NORM_PRIORITY, false));
        threadConfigs.put(ExecutorNames.METHODRETURN, new ThreadCfg(1, Thread.NORM_PRIORITY, false));
    }

    public int getSignalThreadPoolSize() {
        return threadConfigs.get(ExecutorNames.SIGNAL).poolSize();
    }

    public int getErrorThreadPoolSize() {
        return threadConfigs.get(ExecutorNames.ERROR).poolSize();
    }

    public int getMethodCallThreadPoolSize() {
        return threadConfigs.get(ExecutorNames.METHODCALL).poolSize();
    }

    public int getMethodReturnThreadPoolSize() {
        return threadConfigs.get(ExecutorNames.METHODRETURN).poolSize();
    }

    public int getSignalThreadPriority() {
        return threadConfigs.get(ExecutorNames.SIGNAL).priority();
    }

    public int getMethodCallThreadPriority() {
        return threadConfigs.get(ExecutorNames.METHODCALL).priority();
    }

    public int getErrorThreadPriority() {
        return threadConfigs.get(ExecutorNames.ERROR).priority();
    }

    public int getMethodReturnThreadPriority() {
        return threadConfigs.get(ExecutorNames.METHODRETURN).priority();
    }

    public IThreadPoolRetryHandler getRetryHandler() {
        return retryHandler;
    }

    public boolean isSignalVirtualThreads() {
        return threadConfigs.get(ExecutorNames.SIGNAL).virtual();
    }

    public boolean isErrorVirtualThreads() {
        return threadConfigs.get(ExecutorNames.ERROR).virtual();
    }

    public boolean isMethodCallVirtualThreads() {
        return threadConfigs.get(ExecutorNames.METHODCALL).virtual();
    }

    public boolean isMethodReturnVirtualThreads() {
        return threadConfigs.get(ExecutorNames.METHODRETURN).virtual();
    }

    public boolean isVirtual(ExecutorNames _type) {
        return Optional.ofNullable(_type).map(p -> threadConfigs.get(p).virtual()).orElseThrow();
    }

    public int getPoolSize(ExecutorNames _type) {
        return Optional.ofNullable(_type).map(p -> threadConfigs.get(p).poolSize()).orElseThrow();
    }

    public int getPriority(ExecutorNames _type) {
        return Optional.ofNullable(_type).map(p -> threadConfigs.get(p).priority()).orElseThrow();
    }

    void setSignalThreadPoolSize(int _signalThreadPoolSize) {
        threadConfigs.compute(ExecutorNames.SIGNAL, (t, u) -> new ThreadCfg(_signalThreadPoolSize, u.priority(), u.virtual()));
    }

    void setErrorThreadPoolSize(int _errorThreadPoolSize) {
        threadConfigs.compute(ExecutorNames.ERROR, (t, u) -> new ThreadCfg(_errorThreadPoolSize, u.priority(), u.virtual()));
    }

    void setMethodCallThreadPoolSize(int _methodCallThreadPoolSize) {
        threadConfigs.compute(ExecutorNames.METHODCALL, (t, u) -> new ThreadCfg(_methodCallThreadPoolSize, u.priority(), u.virtual()));
    }

    void setMethodReturnThreadPoolSize(int _methodReturnThreadPoolSize) {
        threadConfigs.compute(ExecutorNames.METHODRETURN, (t, u) -> new ThreadCfg(_methodReturnThreadPoolSize, u.priority(), u.virtual()));
    }

    void setSignalThreadPriority(int _signalThreadPriority) {
        threadConfigs.compute(ExecutorNames.SIGNAL, (t, u) -> new ThreadCfg(u.poolSize(), _signalThreadPriority, u.virtual()));
    }

    void setMethodCallThreadPriority(int _methodCallThreadPriority) {
        threadConfigs.compute(ExecutorNames.METHODCALL, (t, u) -> new ThreadCfg(u.poolSize(), _methodCallThreadPriority, u.virtual()));
    }

    void setErrorThreadPriority(int _errorThreadPriority) {
        threadConfigs.compute(ExecutorNames.ERROR, (t, u) -> new ThreadCfg(u.poolSize(), _errorThreadPriority, u.virtual()));
    }

    void setMethodReturnThreadPriority(int _methodReturnThreadPriority) {
        threadConfigs.compute(ExecutorNames.METHODRETURN, (t, u) -> new ThreadCfg(u.poolSize(), _methodReturnThreadPriority, u.virtual()));
    }

    void setRetryHandler(IThreadPoolRetryHandler _retryHandler) {
        retryHandler = _retryHandler;
    }

    void setSignalVirtualThreads(boolean _signalVirtualThreads) {
        threadConfigs.compute(ExecutorNames.SIGNAL, (t, u) -> new ThreadCfg(u.poolSize(), u.priority(), _signalVirtualThreads));
    }

    void setErrorVirtualThreads(boolean _errorVirtualThreads) {
        threadConfigs.compute(ExecutorNames.ERROR, (t, u) -> new ThreadCfg(u.poolSize(), u.priority(), _errorVirtualThreads));
    }

    void setMethodCallVirtualThreads(boolean _methodCallVirtualThreads) {
        threadConfigs.compute(ExecutorNames.METHODCALL, (t, u) -> new ThreadCfg(u.poolSize(), u.priority(), _methodCallVirtualThreads));
    }

    void setMethodReturnVirtualThreads(boolean _methodReturnVirtualThreads) {
        threadConfigs.compute(ExecutorNames.METHODRETURN, (t, u) -> new ThreadCfg(u.poolSize(), u.priority(), _methodReturnVirtualThreads));
    }

    private record ThreadCfg(int poolSize, int priority, boolean virtual) {}
}
