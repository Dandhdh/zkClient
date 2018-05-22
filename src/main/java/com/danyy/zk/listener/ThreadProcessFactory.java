package com.danyy.zk.listener;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 监听器回调线程工程类
 */
public class ThreadProcessFactory implements ThreadFactory {
    private AtomicInteger count = new AtomicInteger(0);

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.setName("zkClient-listener-process-" + count.incrementAndGet());
        return thread;
    }
}
