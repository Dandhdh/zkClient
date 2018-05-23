package com.danyy.zk.lock;

import java.util.concurrent.Semaphore;

/**
 * 将当前线程和信号量封装在一起
 */
public class BoundSemaphore {
    private Thread thread;
    private Semaphore semaphore;

    public BoundSemaphore(Thread thread, Semaphore semaphore) {
        this.thread = thread;
        this.semaphore = semaphore;
    }

    public Thread getThread() {
        return thread;
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }
}
