package com.danyy.zk.lock;

import com.danyy.zk.ZkClient;
import com.danyy.zk.listener.StateListener;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 该锁为进程间的锁,该对象线程安全支持多线程调用
 */
public class SimpleLock implements Lock {
    private final static Logger LOGGER = LoggerFactory.getLogger(SimpleLock.class);
    private final ThreadLocal<ReentrantState> currentLock = new ThreadLocal<ReentrantState>();
    private ZkClient client;
    // 锁的目录
    private String lockPath = "/a";
    // 锁目录下的路径分片
    private String seqPath;
    private LockListener lockListener;
    private static SimpleLock simpleLock = new SimpleLock();
    private volatile boolean isInit = false;

    private SimpleLock() {
    }

    /**
     * 获取锁实例
     * @return
     */
    public static SimpleLock getInstance() {
        return simpleLock;
    }

    /**
     * 初始化
     *
     * @param client
     * @param path
     */
    public synchronized void init(ZkClient client, String path) {
        // 是否已经初始化
        if (isInit) {
            LOGGER.warn("Repeat init simpleLock.");
            return;
        }
        this.client = client;
        if (path != null) {
            if (path.indexOf("/") == 0) {
                this.lockPath = path;
            }
            // 判断路径的结尾是不是"/"，然后响应地加上 /1
            if (path.lastIndexOf("/") != path.length() - 1) {
                this.seqPath = this.lockPath + "/1";
            } else {
                this.seqPath = this.lockPath + "1";
            }
        }
        // 不存在路径的话，需要创建
        if (!client.exists(lockPath)) {
            this.client.create(lockPath, CreateMode.PERSISTENT);
        }
        List<String> nodes = client.getChild(this.lockPath, false);

        lockListener = new LockListener(nodes);
        client.listenChild(this.lockPath, lockListener);
        // 监听状态的变化
        client.listenState(Watcher.Event.KeeperState.Expired, new StateListener() {
            @Override
            public void listen(Watcher.Event.KeeperState state) {
                lockListener.interrupt();
            }
        });
        // 初始化成功，设置标志
        isInit = true;
    }

    /**
     * 获得锁，一直等待，该锁不可重入
     * @return
     */
    @Override
    public boolean lock() {
        return this.lock(0);
    }

    /**
     * 获得锁，该锁可重入
     *
     * @param timeout 0 或者 大于0的 毫秒数，当设置为0时，程序将一直等待，直到获取到锁，
     *                当设置大于0时，等待获得锁的最长时间为timeout的值
     * @return 是否获取到锁，当超时时返回false
     */
    @Override
    public boolean lock(long timeout) {
        ReentrantState state = currentLock.get();
        if (state == null) {
            final Semaphore lockObj = new Semaphore(0);
            BoundSemaphore bs = new BoundSemaphore(Thread.currentThread(), lockObj);
            String newPath = this.client.create(this.seqPath, "".getBytes(), CreateMode.EPHEMERAL_SEQUENTIAL);
            state = new ReentrantState(newPath);
            currentLock.set(state);
            String[] paths = newPath.split("/");
            final String seq = paths[paths.length - 1];
            lockListener.addQueue(seq, bs);
            boolean islock = false;
            boolean isException = false;
            try {
                if (timeout >= 1) {
                    islock = lockObj.tryAcquire(timeout, TimeUnit.MILLISECONDS);
                } else {
                    lockObj.acquire();
                    islock = true;
                }
                return islock;
            } catch (InterruptedException e) {
                isException = true;
                this.currentLock.remove();
                throw new LockSessionException("Get lock fail,zookeeper session timeout. " + state.getLockPath());
            } finally {
                if (!islock && !isException) {
                    this.unlock();
                }
            }
        } else {
            state.add();
        }
        return false;
    }

    /**
     * 释放锁
     */
    @Override
    public void unlock() {
        ReentrantState state = currentLock.get();
        if (state != null) {
            int count = state.decrementAndGet();
            if (count < 1) {
                client.delete(state.getLockPath());
                currentLock.remove();
            }
        }
    }

    /**
     * 销毁锁
     */
    @Override
    public void destroy() {
        client.unlintenChild(this.lockPath);
    }
}
