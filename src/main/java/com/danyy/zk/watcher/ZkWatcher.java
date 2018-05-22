package com.danyy.zk.watcher;

import com.danyy.zk.ZkClient;
import com.danyy.zk.ZkClientException;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * zookeeper 事件接收类
 */
public class ZkWatcher implements Watcher, AsyncCallback.ChildrenCallback {
    private final static Logger LOGGER = LoggerFactory.getLogger(ZkWatcher.class);
    private WatcherProcess process;
    private ZkClient zkClient;
    private Semaphore connLock;

    public ZkWatcher(Semaphore connLock, ZkClient zkClient) {
        this.connLock = connLock;
        this.zkClient = zkClient;
    }

    @Override
    public void processResult(int i, String s, Object o, List<String> list) {

    }

    /**
     * 处理事件：会话超时，连接断开，节点变化等
     *
     * @param event
     */
    @Override
    public void process(WatchedEvent event) {
        switch (event.getState()) {
            case ConnectedReadOnly:
            case SyncConnected:
                if (!zkClient.isConnection()) {
                    //连接成功
                    zkClient.setIsConnection(true);
                    /**
                     * 连接成功后释放Semaphore的许可，
                     * 是的其他因为执行acquire()方法而阻塞的线程线程可以获取到许可，
                     * 从而继续执行
                     */
                    connLock.release();
                    LOGGER.warn("Zookeeper connection or retry success......");
                    System.out.println("连接成功");
                }
                break;
            //会话超时
            case Expired:
                this.stateChange(event.getState());
                resetSession();
                break;
            //连接断开
            case Disconnected:
                zkClient.setIsConnection(false);
                this.stateChange(event.getState());
                LOGGER.warn("Zookeeper connection break......");
                break;
            default:
                LOGGER.warn("Zookeeper state: " + event.getState());
                System.out.println("Zookeeper state: " + event.getState());
                break;
        }
        switch (event.getType()) {
            //子节点变化
            case NodeChildrenChanged:
                this.childChange(event.getPath());
                break;
            //节点数据变化
            case NodeDataChanged:
                this.dataChange(event.getPath());
                break;
                default:
                    break;
        }

    }

    /**
     * 重置会话信息
     */
    private void resetSession() {
        LOGGER.warn("Zookeeper session timeout......");
        try {
            zkClient.reconnection();
            LOGGER.warn("Zookeeper session timeout,retry success. ");
        } catch (ZkClientException e) {
            LOGGER.error("Zookeeper reset session faiil.", e);
        }
    }

    /**
     * 数据变化处理
     *
     * @param path
     */
    private void dataChange(String path) {
        try {
            process.dataChange(path);
        } catch (ZkClientException e) {
            LOGGER.error("Data change watcher exception.", e);
        }
    }

    /**
     * 子节点发生变化
     *
     * @param path
     */
    private void childChange(String path) {
        try {
            process.childChange(path, false);
        } catch (ZkClientException e) {
            LOGGER.error("Child change watcher exception.", e);
        }
    }

    /**
     * 状态变化监听
     *
     * @param state
     */
    private void stateChange(Event.KeeperState state) {
        process.listen(state);
    }

    /**
     * 添加zookeeper事件处理类
     *
     * @param process
     */
    public void setWatcherProcess(WatcherProcess process) {
        this.process = process;
    }
}

