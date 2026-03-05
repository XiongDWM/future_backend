package com.xiongdwm.future_backend.utils.cache;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;
import java.util.function.BiConsumer;

public class LRUCache<K, V> {

    /** 双向链表节点 — 替代 PriorityQueue，remove/moveToTail 均 O(1) */
    private static class Node<K, V> {
        K key;
        V value;
        long expireTime;
        Node<K, V> prev, next;

        Node(K key, V value, long expireTime) {
            this.key = key;
            this.value = value;
            this.expireTime = expireTime;
        }

        @Override
        public String toString() {
            return "Entry{key=" + key + ", value=" + value + ", expireTime=" + expireTime + '}';
        }
    }

    private final Node<K, V> head = new Node<>(null, null, 0); // sentinel
    private final Node<K, V> tail = new Node<>(null, null, 0); // sentinel

    private void linkToTail(Node<K, V> node) {
        node.prev = tail.prev;
        node.next = tail;
        tail.prev.next = node;
        tail.prev = node;
    }

    private void unlink(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
        node.prev = null;
        node.next = null;
    }

    private void moveToTail(Node<K, V> node) {
        unlink(node);
        linkToTail(node);
    }

    private final int capacity;
    private final long expireTimeLimit;
    private final HashMap<K, Node<K, V>> map;          // 外部已有锁，不需要 ConcurrentHashMap
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock  = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
    private static final long DEFAULT_EXPIRE_TIME = 5 * 60 * 1000;
    private static final int DEFAULT_CAPACITY = 64;
    private volatile K latest;
    private volatile BiConsumer<K, V> evictionListener;

    private static final ScheduledExecutorService SHARED_SCHEDULER =
            Executors.newScheduledThreadPool(1, r -> {
                Thread t = new Thread(r, "lru-cache-cleaner");
                t.setDaemon(true);
                return t;
            });
    private final ScheduledFuture<?> cleanupTask;

    // constructors
    public LRUCache() {
        this(DEFAULT_CAPACITY, DEFAULT_EXPIRE_TIME);
    }

    public LRUCache(int capacity) {
        this(capacity, DEFAULT_EXPIRE_TIME);
    }

    public LRUCache(long expireTime) {
        this(DEFAULT_CAPACITY, expireTime);
    }

    public LRUCache(int capacity, long expireTimeLimit) {
        this.capacity = capacity;
        this.expireTimeLimit = expireTimeLimit;
        this.map = new HashMap<>(capacity);
        head.next = tail;
        tail.prev = head;

        long interval = Math.max(expireTimeLimit / 4, 1000);   // 清理间隔取 1/4 TTL，兼顾精度和开销
        this.cleanupTask = SHARED_SCHEDULER.scheduleAtFixedRate(
                this::clearExpiredEntries, interval, interval, TimeUnit.MILLISECONDS);
    }

    /** 设置过期/淘汰回调，当条目被自动清理时触发 */
    public void setEvictionListener(BiConsumer<K, V> listener) {
        this.evictionListener = listener;
    }

    public V get(K key) {
        // 滑动续期必须写，直接拿写锁，避免 read→write 的 TOCTOU 竞态
        writeLock.lock();
        try {
            Node<K, V> node = map.get(key);
            if (node == null || System.currentTimeMillis() >= node.expireTime) {
                return null;
            }
            node.expireTime = System.currentTimeMillis() + expireTimeLimit;
            moveToTail(node);
            latest = key;
            return node.value;
        } finally {
            writeLock.unlock();
        }
    }

    public V peek(K key) {
        readLock.lock();
        try {
            Node<K, V> node = map.get(key);
            if (node == null || System.currentTimeMillis() >= node.expireTime) {
                return null;
            }
            return node.value;
        } finally {
            readLock.unlock();
        }
    }

    public List<K> getAllKeys() {
        readLock.lock();
        try {
            if (map.isEmpty()) return Collections.emptyList();
            return new ArrayList<>(map.keySet());
        } finally {
            readLock.unlock();
        }
    }

    public void put(K key, V value) {
        writeLock.lock();
        try {
            long now = System.currentTimeMillis();
            Node<K, V> existing = map.get(key);
            if (existing != null) {
                existing.value = value;
                existing.expireTime = now + expireTimeLimit;
                moveToTail(existing);
                latest = key;
                return;
            }
            if (map.size() >= capacity) {
                // 淘汰链表头部（最老的）
                Node<K, V> eldest = head.next;
                if (eldest != tail) {
                    unlink(eldest);
                    map.remove(eldest.key);
                }
            }
            Node<K, V> node = new Node<>(key, value, now + expireTimeLimit);
            map.put(key, node);
            linkToTail(node);
            latest = key;
        } finally {
            writeLock.unlock();
        }
    }

    public V remove(K key) {
        writeLock.lock();
        try {
            Node<K, V> node = map.remove(key);
            if (node != null) {
                unlink(node);
                return node.value;
            }
            return null;
        } finally {
            writeLock.unlock();
        }
    }

    public Map.Entry<K, V> peek() {
        readLock.lock();
        try {
            if (latest == null) return null;
            Node<K, V> node = map.get(latest);
            return node == null ? null : new AbstractMap.SimpleEntry<>(node.key, node.value);
        } finally {
            readLock.unlock();
        }
    }

    public List<V> getAllValues() {
        readLock.lock();
        try {
            if (map.isEmpty()) return Collections.emptyList();
            List<V> values = new ArrayList<>(map.size());
            map.forEach((k, node) -> values.add(node.value));
            return values;
        } finally {
            readLock.unlock();
        }
    }

    public List<Map.Entry<K, V>> getAllKV() {
        readLock.lock();
        try {
            if (map.isEmpty()) return Collections.emptyList();
            List<Map.Entry<K, V>> entries = new ArrayList<>(map.size());
            map.forEach((k, node) -> entries.add(new AbstractMap.SimpleEntry<>(node.key, node.value)));
            return entries;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * 从链表头部扫描过期节点 — 头部最老，遇到未过期即可 break，无需全表扫描。
     */
    private void clearExpiredEntries() {
        List<Node<K, V>> evicted = null;
        writeLock.lock();
        try {
            long now = System.currentTimeMillis();
            Node<K, V> cur = head.next;
            while (cur != tail) {
                if (now < cur.expireTime) break;    // 后面的更新，不用继续
                Node<K, V> next = cur.next;
                unlink(cur);
                map.remove(cur.key);
                if (evictionListener != null) {
                    if (evicted == null) evicted = new ArrayList<>();
                    evicted.add(cur);
                }
                cur = next;
            }
        } finally {
            writeLock.unlock();
        }
        // 在锁外触发回调，避免死锁
        if (evicted != null) {
            var listener = this.evictionListener;
            for (var node : evicted) {
                try { listener.accept(node.key, node.value); } catch (Exception ignored) {}
            }
        }
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean isFull() {
        return map.size() >= capacity;
    }

    public int size() {
        readLock.lock();
        try { return map.size(); }
        finally { readLock.unlock(); }
    }

    public void shutdown() {
        cleanupTask.cancel(false);
        writeLock.lock();
        try {
            map.clear();
            head.next = tail;
            tail.prev = head;
        } finally {
            writeLock.unlock();
        }
    }
}
