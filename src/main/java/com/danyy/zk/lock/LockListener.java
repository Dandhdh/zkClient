package com.danyy.zk.lock;

import com.danyy.zk.ZkClientException;
import com.danyy.zk.listener.Listener;
import org.apache.zookeeper.Watcher;

import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Semaphore;

/**
 * 锁的监听回调函数
 */
public class LockListener implements Listener {

    private Map<String, BoundSemaphore> waitLocks = new ConcurrentHashMap<String, BoundSemaphore>();
    // 在锁范围的所有znode的集合
    private ConcurrentSkipListMap<String, Boolean> totalLockNode = new ConcurrentSkipListMap<String, Boolean>(new NodeComparator<String>());

    public LockListener(List<String> nodes) {
        if (nodes != null) {
            for (String node : nodes) {
                totalLockNode.put(node, true);
            }
        }
    }

    @Override
    public void listen(String path, Watcher.Event.EventType eventType, byte[] data) throws ZkClientException, SocketException {
        String[] node = path.split("/");
        String seq = node[node.length - 1];
        if (eventType == Watcher.Event.EventType.NodeCreated) {
            totalLockNode.put(seq, true);
        } else {
            //删除节点事件
            totalLockNode.remove(seq);
        }
        this.release();
    }

    /**
     * 释放锁
     */
    private void release() {
        Map.Entry<String, Boolean> minEntry = totalLockNode.firstEntry();
        if (minEntry != null) {
            String minNode = minEntry.getKey();
            if (waitLocks.containsKey(minNode)) {
                Semaphore lock = waitLocks.get(minNode).getSemaphore();
                lock.release();
                waitLocks.remove(minNode);
            }
        }
    }

    /**
     * 添加等待队列
     *
     * @param path 锁节点
     * @param bs   信号量对象
     */
    public void addQueue(String path, BoundSemaphore bs) {
        waitLocks.put(path, bs);
        if (totalLockNode.containsKey(path)) {//监听事件早于addQueue进来
            this.release();
        }
    }

    /**
     * 中断所有等待锁的线程
     */
    public void interrupt() {
        Map<String, BoundSemaphore> tmp = new HashMap<String, BoundSemaphore>(waitLocks);
        waitLocks.clear();
        for (Map.Entry<String, BoundSemaphore> entry : tmp.entrySet()) {
            Thread thread = entry.getValue().getThread();
            thread.interrupt();
        }
    }
}
