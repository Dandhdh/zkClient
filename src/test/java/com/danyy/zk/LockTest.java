package com.danyy.zk;


import com.danyy.zk.lock.Lock;

public class LockTest {
    public static void main(String[] args) throws InterruptedException {
        final ZkClient zk = new ZkClient("127.0.0.1:2181");
        // 获取到锁对象，这里默认为SimpleLock
        final Lock lock = zk.getLock("/lock/a/b");
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("1---------start");
                lock.lock();
                System.out.println("1---------getLock");
                try {
                    Thread.sleep(2000);
                    lock.lock();
                    System.out.println("1---------re getLock");
                    Thread.sleep(3000);
                    lock.unlock();
                    System.out.println("1-2--------unlock");
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("1-1--------unlock");
                lock.unlock();
            }
        }).start();
        Thread.sleep(1000);
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("2----------start");
                boolean s = lock.lock();
                System.out.println("2----------getLock---"+s);
                try {
                    Thread.sleep(3000);
                    System.out.println("2---------unlock");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                lock.unlock();
            }
        }).start();
        Thread.sleep(100000);
    }
}
