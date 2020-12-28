package org.freedesktop.dbus.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NameableThreadFactory implements ThreadFactory {

    private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
    private final ThreadGroup          group;
    private final AtomicInteger        threadNumber = new AtomicInteger(1);
    private final String               namePrefix;

    private final boolean              daemonizeThreads;

    /**
     * Create a new ThreadFactory instance.
     * The thread name is created like this:
     * _name + THREAD_NUMBER
     * e.g: connectionPool-1
     * If _name is null or blank, UnnamedThreadPool-POOL_NUMBER-thread-THREAD_NUMBER will be used.
     *
     * @param _name prefix for all thread names
     * @param _daemonizeThreads turn all created threads to daemon threads
     */
    public NameableThreadFactory(String _name, boolean _daemonizeThreads) {
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        namePrefix = Util.isBlank(_name) ? "UnnamedThreadPool-" + POOL_NUMBER.getAndIncrement() + "-thread-" : _name;
        daemonizeThreads = _daemonizeThreads;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
        t.setDaemon(daemonizeThreads);

        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
    }

}
