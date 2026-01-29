# 🎯 Senior Java Engineer: 2-Day Concurrency & LLD Interview Mastery

> **For Engineers with 5-15 Years Experience** | **LLD & Concurrency Focus** | **January 2026**

---

## 📌 Who This Plan Is For

You're a **senior Java engineer** preparing for:
- ✅ Low-Level Design (LLD) rounds at FAANG/product companies
- ✅ Concurrency-heavy system design questions
- ✅ Senior/Staff engineer positions where **internals matter**

**What You Already Know:**
- Thread creation, basic synchronization, ExecutorService
- Standard collections and common patterns
- How to write multithreaded code that works

**What You Need to Master:**
- **Java Memory Model (JMM)** - How volatile/synchronized actually work
- **ThreadPool internals** - How ExecutorService works under the hood
- **Lock-free programming** - CAS, Atomic classes, why they matter
- **Designing concurrent systems** - Thread-safe caches, rate limiters, queues
- **Modern Java** - Virtual Threads (Project Loom), structured concurrency

---

## 🎯 Learning Philosophy

> **"Don't just use ConcurrentHashMap - understand its segment locking, CAS operations, and when NOT to use it."**

This plan focuses on:
1. **Internals over syntax** - "How does it work?" not "How do I use it?"
2. **Design patterns** - Common LLD questions require applying concurrency patterns
3. **Trade-offs** - Every tool has costs; senior engineers justify their choices
4. **Modern Java** - Virtual Threads are appearing in 2025/2026 interviews

---

## 📚 Pre-Work: What to Skip

**Skip These (You Already Know):**
- ❌ Basic Thread creation (Thread vs Runnable)
- ❌ Simple synchronized examples
- ❌ Thread lifecycle diagrams you've seen 100 times

**Focus Here Instead:**
- ✅ Memory models and happens-before relationships
- ✅ Lock implementations and their trade-offs
- ✅ Designing production-grade concurrent systems

---

## 📅 DAY 1: Internals & Advanced Concepts

**Goal:** Deep understanding of JMM, synchronization mechanisms, and how concurrent utilities actually work

---

### ⏰ Morning (3 hours): Java Memory Model Deep Dive

#### 🎥 Primary Video Resources

**Must-Watch (in order):**

1. **Defog Tech - Java Concurrency Playlist** ⭐ **START HERE**
   - Search YouTube: `"Defog Tech Java concurrency"`
   - Highly recommended by senior engineers on LinkedIn/Reddit
   - Focus on: Java Memory Model, volatile, synchronized internals
   - ⏱️ Watch: JMM, volatile vs synchronized (45-60 min)
   
2. **Java Memory Model Fundamentals**
   - **Alternative:** Search for "Java Memory Model happens-before" talks
   - Look for presentations by Doug Lea, Brian Goetz (JMM spec authors)
   - ⏱️ 30-45 minutes

**Why This Matters for LLD:**
When designing thread-safe systems, you'll be asked:
- "Why is volatile not enough for a counter?"
- "How would you implement a thread-safe singleton?"
- "Explain double-checked locking and why it needs volatile"

---

#### 📖 Essential Reading

**1. Java Memory Model - Happens-Before**

Read this authoritative source:
- **JSR-133 FAQ**: https://www.cs.umd.edu/~pugh/java/memoryModel/jsr-133-faq.html
- Focus on: happens-before rules, volatile semantics, final field semantics

**2. Doug Lea's Concurrency Interest Site**
- http://gee.cs.oswego.edu/dl/cpj/jmm.html
- Technical but essential for understanding memory barriers

---

#### 💻 Code Study: Understanding Internals

##### 1️⃣ The Java Memory Model - What Actually Happens

```java
/**
 * CRITICAL CONCEPT: Memory Visibility
 * Without JMM guarantees, this code can run forever even though 
 * another thread sets running = false
 */
class MemoryVisibilityProblem {
    // Problem: Not volatile
    private boolean running = true;
    
    public void runTask() {
        // Thread might cache 'running' in CPU register
        // and never see updates from other threads
        while (running) {
            // do work
        }
        System.out.println("Stopped!"); // May never execute
    }
    
    public void stop() {
        running = false; // Write might stay in CPU cache
    }
}

/**
 * Solution 1: volatile
 * Guarantees: 
 * 1. Writes are immediately visible to all threads (memory barrier)
 * 2. Prevents instruction reordering around volatile operations
 */
class VolatileCorrect {
    private volatile boolean running = true;
    
    public void runTask() {
        while (running) { // Always reads from main memory
            // do work
        }
    }
    
    public void stop() {
        running = false; // Immediately written to main memory
    }
}
```

**Interview Question:**
Q: "Why can't we use volatile for a counter that's incremented by multiple threads?"

```java
class VolatileMisuse {
    private volatile int counter = 0; // volatile doesn't help here!
    
    public void increment() {
        counter++; // NOT ATOMIC! Three operations:
                   // 1. Read counter
                   // 2. Add 1
                   // 3. Write back
                   // Race condition between 1 and 3!
    }
}

// Correct solutions:
class AtomicCorrect {
    private AtomicInteger counter = new AtomicInteger(0);
    
    public void increment() {
        counter.incrementAndGet(); // Single atomic operation using CAS
    }
}

class SynchronizedCorrect {
    private int counter = 0;
    
    public synchronized void increment() {
        counter++; // Protected by monitor lock
    }
}
```

##### 2️⃣ happens-before Relationship

```java
/**
 * CRITICAL: happens-before is the foundation of JMM
 * 
 * Key Rules:
 * 1. Program order: Each action happens-before subsequent actions in same thread
 * 2. Monitor lock: Unlock happens-before every subsequent lock on same monitor
 * 3. Volatile: Write to volatile happens-before every subsequent read
 * 4. Thread start: thread.start() happens-before any action in started thread
 * 5. Thread join: Actions in thread happen-before successful join
 */

class HappensBeforeExample {
    private int x = 0;
    private volatile boolean ready = false;
    
    // Thread 1
    public void writer() {
        x = 42;              // (1)
        ready = true;        // (2) volatile write
    }
    
    // Thread 2
    public void reader() {
        if (ready) {         // (3) volatile read
            System.out.println(x);  // (4)
        }
    }
    
    /**
     * JMM Guarantee:
     * (1) happens-before (2) - program order
     * (2) happens-before (3) - volatile rule
     * (3) happens-before (4) - program order
     * Therefore: (1) happens-before (4)
     * 
     * Result: reader() will see x = 42, NOT 0
     */
}
```

##### 3️⃣ Double-Checked Locking (Classic Interview Question)

```java
/**
 * BROKEN Implementation (Pre-Java 5)
 * Used to fail due to instruction reordering
 */
class BrokenSingleton {
    private static BrokenSingleton instance;
    
    public static BrokenSingleton getInstance() {
        if (instance == null) {              // (1) Check
            synchronized (BrokenSingleton.class) {
                if (instance == null) {       // (2) Check
                    instance = new BrokenSingleton(); // (3) PROBLEM!
                }
            }
        }
        return instance;
    }
    
    /**
     * Why it breaks:
     * Line (3) is actually 3 operations:
     * a) Allocate memory
     * b) Invoke constructor
     * c) Assign address to 'instance'
     * 
     * JVM can reorder to: a -> c -> b
     * Thread 1 does a -> c
     * Thread 2 sees instance != null, returns half-initialized object!
     */
}

/**
 * CORRECT Implementation (Java 5+)
 * volatile prevents reordering
 */
class CorrectSingleton {
    private static volatile CorrectSingleton instance;
    
    public static CorrectSingleton getInstance() {
        if (instance == null) {
            synchronized (CorrectSingleton.class) {
                if (instance == null) {
                    instance = new CorrectSingleton();
                    // volatile ensures constructor completes before
                    // assignment is visible to other threads
                }
            }
        }
        return instance;
    }
}

/**
 * BEST Implementation: Initialization-on-demand holder idiom
 * Uses class loader guarantees, no synchronization overhead
 */
class BestSingleton {
    private BestSingleton() {}
    
    private static class Holder {
        // JVM guarantees thread-safe initialization of static fields
        static final BestSingleton INSTANCE = new BestSingleton();
    }
    
    public static BestSingleton getInstance() {
        return Holder.INSTANCE;
    }
}
```

---

### 🌆 Afternoon (3 hours): Lock Internals & Concurrent Data Structures

#### 🎥 Video Focus: ThreadPool & Executor Internals

**Michael Pogrebinsky - Java Multithreading Course** (Udemy)
- ⭐ 4.6/5 (13,000+ ratings)
- **Skip:** Sections 1-3 (you know this)
- **Watch:** Sections 5-8:
  - ThreadPoolExecutor internals
  - Performance optimization
  - Lock-free algorithms
  - Virtual Threads (Java 21)
- ⏱️ 2-3 hours (watch at 1.5x speed)

---

#### 💻 Deep Dive: How Concurrent Utilities Work

##### 1️⃣ ThreadPoolExecutor Internals

```java
import java.util.concurrent.*;

/**
 * Understanding ThreadPoolExecutor Design
 * 
 * Key Components:
 * 1. Work Queue: BlockingQueue<Runnable>
 * 2. Worker threads: Set<Worker>
 * 3. Core vs Max pool size
 * 4. Keep-alive time
 * 5. Rejection policy
 */

class ThreadPoolInternalsDemo {
    public static void main(String[] args) {
        /**
         * Internal flow:
         * 1. Task submitted via execute()
         * 2. If running threads < corePoolSize → create new thread
         * 3. Else if queue not full → add to queue
         * 4. Else if running threads < maxPoolSize → create thread
         * 5. Else → reject (apply RejectedExecutionHandler)
         */
        
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2,                              // corePoolSize
            4,                              // maximumPoolSize
            60L, TimeUnit.SECONDS,          // keepAliveTime
            new ArrayBlockingQueue<>(10),   // workQueue
            new ThreadPoolExecutor.CallerRunsPolicy() // rejectionHandler
        );
        
        /**
         * Interview Q: "What happens when you submit 20 tasks?"
         * Answer:
         * - Tasks 1-2: Execute immediately (2 core threads created)
         * - Tasks 3-12: Queue (queue capacity = 10)
         * - Tasks 13-14: Create 2 more threads (max = 4)
         * - Tasks 15-20: Rejected (use CallerRunsPolicy - run in caller thread)
         */
        
        // Custom ThreadFactory for naming/monitoring
        executor.setThreadFactory(new ThreadFactory() {
            private int counter = 0;
            
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "custom-worker-" + counter++);
                t.setDaemon(false);
                t.setPriority(Thread.NORM_PRIORITY);
                return t;
            }
        });
        
        // Proper shutdown
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

/**
 * Interview Q: "How would you size a thread pool?"
 */
class ThreadPoolSizing {
    /**
     * CPU-Bound Tasks:
     * poolSize = numCores + 1
     * (The +1 helps when threads occasionally page fault)
     * 
     * I/O-Bound Tasks:
     * poolSize = numCores * (1 + waitTime/serviceTime)
     * 
     * Example: 
     * - 8 cores
     * - Task: 90% waiting (I/O), 10% CPU
     * - waitTime/serviceTime = 9
     * - poolSize = 8 * (1 + 9) = 80 threads
     */
    
    public static int calculatePoolSize(int numCores, double blockingCoefficient) {
        // blockingCoefficient: 0.0 (CPU-bound) to 0.9 (highly I/O-bound)
        return (int) (numCores / (1 - blockingCoefficient));
    }
}
```

##### 2️⃣ ConcurrentHashMap Internals

```java
import java.util.concurrent.*;

/**
 * ConcurrentHashMap Design Evolution
 * 
 * Java 7: Segment locking (16 segments by default)
 * Java 8+: CAS + synchronized (node-level locking)
 */

class ConcurrentHashMapInternals {
    
    /**
     * Interview Q: "Why is ConcurrentHashMap faster than synchronizedMap?"
     * 
     * Answer:
     * 1. Fine-grained locking (lock per node, not entire map)
     * 2. Lock-free reads (most get() operations)
     * 3. CAS operations for updates
     * 4. No lock for iteration
     */
    
    public void demonstrateInternals() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        
        /**
         * Internal structure (Java 8+):
         * 
         * table[] -> array of Node<K,V>
         * 
         * Each bucket:
         * - If collision → linked list
         * - If list size > 8 → convert to TreeNode (Red-Black tree)
         * 
         * Put operation:
         * 1. Compute hash
         * 2. If bucket empty → CAS to add node (lock-free!)
         * 3. If bucket not empty → synchronized on first node
         * 4. Add to list/tree
         */
        
        map.put("key1", 1);  // Lock-free if bucket empty
        
        /**
         * Resizing:
         * - Incremental resizing (not all at once)
         * - Multiple threads can help resize
         * - Uses forwarding nodes during resize
         */
    }
    
    /**
     * Interview Q: "When would you NOT use ConcurrentHashMap?"
     * 
     * Answers:
     * 1. Need consistent iteration snapshot → CopyOnWriteArrayList
     * 2. Need strict ordering → Collections.synchronizedMap(TreeMap)
     * 3. Single-threaded performance critical → regular HashMap
     * 4. Need transactional updates → use explicit locking
     */
}

/**
 * Building a Simple Lock-Free Data Structure
 */
class LockFreeStack<T> {
    private static class Node<T> {
        final T value;
        Node<T> next;
        
        Node(T value) {
            this.value = value;
        }
    }
    
    private AtomicReference<Node<T>> head = new AtomicReference<>();
    
    public void push(T value) {
        Node<T> newHead = new Node<>(value);
        Node<T> oldHead;
        
        do {
            oldHead = head.get();
            newHead.next = oldHead;
        } while (!head.compareAndSet(oldHead, newHead));
        // CAS: if head is still oldHead, set to newHead
        // If another thread changed head, retry
    }
    
    public T pop() {
        Node<T> oldHead;
        Node<T> newHead;
        
        do {
            oldHead = head.get();
            if (oldHead == null) {
                return null;
            }
            newHead = oldHead.next;
        } while (!head.compareAndSet(oldHead, newHead));
        
        return oldHead.value;
    }
    
    /**
     * Interview insight:
     * - No locks, just CAS operations
     * - ABA problem possible (mitigated with AtomicStampedReference)
     * - Good when contention is low-medium
     * - High contention → spinning wastes CPU
     */
}
```

##### 3️⃣ Lock Implementations Comparison

```java
import java.util.concurrent.locks.*;

class LockComparison {
    
    /**
     * synchronized vs ReentrantLock vs StampedLock
     */
    
    // 1. SYNCHRONIZED: Simple, JVM-optimized
    class SynchronizedExample {
        private int count = 0;
        
        public synchronized void increment() {
            count++;
        }
        
        /**
         * Pros:
         * - Simple syntax
         * - Auto release (even with exceptions)
         * - JVM can optimize (lock elision, biased locking)
         * 
         * Cons:
         * - No tryLock
         * - No interruptible waits
         * - No fairness control
         * - Blocks entire method/block
         */
    }
    
    // 2. REENTRANTLOCK: More features
    class ReentrantLockExample {
        private final ReentrantLock lock = new ReentrantLock(true); // fair
        private int count = 0;
        
        public void increment() {
            lock.lock();
            try {
                count++;
            } finally {
                lock.unlock(); // Must be in finally!
            }
        }
        
        public boolean tryIncrement(long timeout) throws InterruptedException {
            if (lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
                try {
                    count++;
                    return true;
                } finally {
                    lock.unlock();
                }
            }
            return false;
        }
        
        /**
         * Pros:
         * - tryLock (non-blocking attempt)
         * - Timed locks
         * - Interruptible locks
         * - Fairness option
         * - Can check if locked
         * 
         * Cons:
         * - Manual unlock required
         * - More verbose
         * - Slightly more overhead than synchronized
         */
    }
    
    // 3. STAMPEDLOCK: Optimistic reads (Java 8+)
    class StampedLockExample {
        private final StampedLock lock = new StampedLock();
        private int count = 0;
        
        public void increment() {
            long stamp = lock.writeLock();
            try {
                count++;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        public int getCount() {
            // Optimistic read (no lock!)
            long stamp = lock.tryOptimisticRead();
            int currentCount = count;
            
            if (!lock.validate(stamp)) {
                // Someone wrote, need real read lock
                stamp = lock.readLock();
                try {
                    currentCount = count;
                } finally {
                    lock.unlockRead(stamp);
                }
            }
            return currentCount;
        }
        
        /**
         * Pros:
         * - Optimistic reads (very fast for read-heavy)
         * - Better performance than ReadWriteLock
         * 
         * Cons:
         * - Not reentrant
         * - Complex API
         * - Can cause deadlock if misused
         * 
         * Use when: 90%+ reads, rare writes
         */
    }
}
```

---

## 📅 DAY 2: LLD Patterns & Interview Problems

**Goal:** Apply concurrency knowledge to design real systems and solve interview problems

---

### ⏰ Morning (3 hours): Design Patterns for LLD

#### 🎨 Common LLD Questions with Concurrency

##### 1️⃣ Design a Thread-Safe Cache with TTL

```java
import java.util.concurrent.*;
import java.util.*;

/**
 * Interview Q: "Design a thread-safe cache with:
 * - get(key)
 * - put(key, value, ttl)
 * - Automatic expiration
 * - LRU eviction when full"
 */

class TTLCache<K, V> {
    private static class CacheEntry<V> {
        final V value;
        final long expiryTime;
        
        CacheEntry(V value, long ttlMillis) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + ttlMillis;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
    
    private final int maxSize;
    private final ConcurrentHashMap<K, CacheEntry<V>> cache;
    private final LinkedHashMap<K, Long> accessOrder;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final ScheduledExecutorService cleaner;
    
    public TTLCache(int maxSize) {
        this.maxSize = maxSize;
        this.cache = new ConcurrentHashMap<>();
        this.accessOrder = new LinkedHashMap<K, Long>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, Long> eldest) {
                return size() > maxSize;
            }
        };
        
        // Background cleanup every 5 seconds
        this.cleaner = Executors.newSingleThreadScheduledExecutor();
        cleaner.scheduleAtFixedRate(this::cleanup, 5, 5, TimeUnit.SECONDS);
    }
    
    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        
        if (entry == null || entry.isExpired()) {
            cache.remove(key);
            lock.writeLock().lock();
            try {
                accessOrder.remove(key);
            } finally {
                lock.writeLock().unlock();
            }
            return null;
        }
        
        // Update access order
        lock.writeLock().lock();
        try {
            accessOrder.put(key, System.currentTimeMillis());
        } finally {
            lock.writeLock().unlock();
        }
        
        return entry.value;
    }
    
    public void put(K key, V value, long ttlMillis) {
        lock.writeLock().lock();
        try {
            if (accessOrder.size() >= maxSize && !accessOrder.containsKey(key)) {
                // Remove LRU entry
                K lruKey = accessOrder.keySet().iterator().next();
                accessOrder.remove(lruKey);
                cache.remove(lruKey);
            }
            
            cache.put(key, new CacheEntry<>(value, ttlMillis));
            accessOrder.put(key, System.currentTimeMillis());
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private void cleanup() {
        cache.forEach((key, entry) -> {
            if (entry.isExpired()) {
                cache.remove(key);
                lock.writeLock().lock();
                try {
                    accessOrder.remove(key);
                } finally {
                    lock.writeLock().unlock();
                }
            }
        });
    }
    
    public void shutdown() {
        cleaner.shutdown();
    }
    
    /**
     * Interview Discussion Points:
     * 
     * 1. Why ConcurrentHashMap + separate lock?
     *    - CHM for concurrent reads
     *    - Lock for LinkedHashMap (not thread-safe)
     * 
     * 2. Why ReadWriteLock?
     *    - get() is read-heavy
     *    - Multiple readers can access simultaneously
     * 
     * 3. Alternative designs:
     *    - Guava Cache (production use)
     *    - Caffeine (better performance)
     *    - Redis (distributed)
     * 
     * 4. Trade-offs:
     *    - Scheduled cleanup vs lazy cleanup
     *    - Memory overhead of dual structures
     *    - Lock contention on access order updates
     */
}
```

##### 2️⃣ Design a Rate Limiter

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Interview Q: "Design a rate limiter that allows N requests per second"
 * 
 * Common algorithms:
 * 1. Token Bucket
 * 2. Leaky Bucket
 * 3. Fixed Window
 * 4. Sliding Window Log
 */

// Implementation 1: Token Bucket (Recommended)
class TokenBucketRateLimiter {
    private final long capacity;         // Max tokens
    private final long refillRate;       // Tokens per second
    private final AtomicLong tokens;
    private final AtomicLong lastRefillTime;
    
    public TokenBucketRateLimiter(long requestsPerSecond) {
        this.capacity = requestsPerSecond;
        this.refillRate = requestsPerSecond;
        this.tokens = new AtomicLong(requestsPerSecond);
        this.lastRefillTime = new AtomicLong(System.nanoTime());
    }
    
    public boolean allowRequest() {
        refill();
        
        long currentTokens;
        do {
            currentTokens = tokens.get();
            if (currentTokens <= 0) {
                return false; // Rate limit exceeded
            }
        } while (!tokens.compareAndSet(currentTokens, currentTokens - 1));
        
        return true;
    }
    
    private void refill() {
        long now = System.nanoTime();
        long lastRefill = lastRefillTime.get();
        long elapsedNanos = now - lastRefill;
        long tokensToAdd = (elapsedNanos * refillRate) / 1_000_000_000L;
        
        if (tokensToAdd > 0) {
            if (lastRefillTime.compareAndSet(lastRefill, now)) {
                long currentTokens;
                long newTokens;
                do {
                    currentTokens = tokens.get();
                    newTokens = Math.min(capacity, currentTokens + tokensToAdd);
                } while (!tokens.compareAndSet(currentTokens, newTokens));
            }
        }
    }
    
    /**
     * Interview Q: "Why AtomicLong instead of synchronized?"
     * 
     * Answer:
     * - Lock-free, better under high contention
     * - No thread blocking
     * - CAS is faster than lock acquisition
     * 
     * Trade-off:
     * - CAS retry loop can waste CPU if contention is extreme
     * - For very high contention, striped locks might be better
     */
}

// Implementation 2: Sliding Window Log (More accurate)
class SlidingWindowRateLimiter {
    private final int maxRequests;
    private final long windowMillis;
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>> requestLogs;
    
    public SlidingWindowRateLimiter(int maxRequests, long windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000;
        this.requestLogs = new ConcurrentHashMap<>();
    }
    
    public boolean allowRequest(String userId) {
        long now = System.currentTimeMillis();
        ConcurrentLinkedQueue<Long> log = requestLogs.computeIfAbsent(
            userId, 
            k -> new ConcurrentLinkedQueue<>()
        );
        
        // Remove expired entries
        while (!log.isEmpty() && now - log.peek() > windowMillis) {
            log.poll();
        }
        
        if (log.size() < maxRequests) {
            log.offer(now);
            return true;
        }
        
        return false;
    }
    
    /**
     * Interview Discussion:
     * 
     * Pros:
     * - Very accurate
     * - No burst issues
     * 
     * Cons:
     * - Memory intensive (stores every request timestamp)
     * - Cleanup overhead
     * 
     * When to use:
     * - Strict rate limiting required
     * - Can afford memory cost
     * - Moderate request volume
     */
}
```

##### 3️⃣ Design a Thread-Safe Queue with Priority

```java
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/**
 * Interview Q: "Design a blocking queue that supports:
 * - Priority-based dequeuing
 * - Blocking put/take
 * - Thread-safe"
 */

class PriorityBlockingQueueImpl<T> {
    private final PriorityQueue<PriorityItem<T>> queue;
    private final int capacity;
    private final Lock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();
    
    private static class PriorityItem<T> implements Comparable<PriorityItem<T>> {
        final T item;
        final int priority;
        
        PriorityItem(T item, int priority) {
            this.item = item;
            this.priority = priority;
        }
        
        @Override
        public int compareTo(PriorityItem<T> other) {
            return Integer.compare(other.priority, this.priority); // Higher first
        }
    }
    
    public PriorityBlockingQueueImpl(int capacity) {
        this.capacity = capacity;
        this.queue = new PriorityQueue<>(capacity);
    }
    
    public void put(T item, int priority) throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() >= capacity) {
                notFull.await(); // Block until space available
            }
            
            queue.offer(new PriorityItem<>(item, priority));
            notEmpty.signal(); // Wake up one waiting taker
        } finally {
            lock.unlock();
        }
    }
    
    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                notEmpty.await(); // Block until item available
            }
            
            T item = queue.poll().item;
            notFull.signal(); // Wake up one waiting putter
            return item;
        } finally {
            lock.unlock();
        }
    }
    
    public boolean offer(T item, int priority, long timeout, TimeUnit unit) 
            throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        lock.lock();
        try {
            while (queue.size() >= capacity) {
                if (nanos <= 0) {
                    return false;
                }
                nanos = notFull.awaitNanos(nanos);
            }
            
            queue.offer(new PriorityItem<>(item, priority));
            notEmpty.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Interview Q: "Why not use Java's PriorityBlockingQueue?"
     * 
     * Answer:
     * - PriorityBlockingQueue is unbounded (no capacity limit)
     * - This custom impl shows understanding of:
     *   - Condition variables
     *   - Producer-consumer pattern
     *   - Proper lock management
     *   - Signal/await semantics
     */
}
```

---

### 🌆 Afternoon (2.5 hours): Modern Java & Interview Problems

#### 🚀 Virtual Threads (Project Loom) - Critical for 2026 Interviews

```java
import java.util.concurrent.*;

/**
 * Virtual Threads (Java 21+)
 * 
 * Key Interview Points:
 * 1. Lightweight (millions possible vs thousands of platform threads)
 * 2. Managed by JVM, not OS
 * 3. Perfect for I/O-bound tasks
 * 4. Should NOT use thread pools
 */

class VirtualThreadsDemo {
    
    // Old way: Platform thread pool
    public void traditionalApproach() {
        ExecutorService executor = Executors.newFixedThreadPool(100);
        
        for (int i = 0; i < 10000; i++) {
            executor.submit(() -> {
                // I/O operation blocks precious platform thread
                callExternalAPI();
            });
        }
        // Problem: Only 100 threads, queue builds up
    }
    
    // New way: Virtual threads
    public void virtualThreadApproach() {
        // Don't use thread pool!
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 10000; i++) {
                executor.submit(() -> {
                    // Virtual thread is cheap, blocks don't matter
                    callExternalAPI();
                });
            }
        }
        // Can handle 10,000+ concurrent I/O operations easily
    }
    
    // Even simpler
    public void directVirtualThread() {
        Thread.startVirtualThread(() -> {
            callExternalAPI();
        });
    }
    
    private void callExternalAPI() {
        // Simulated I/O
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
    }
    
    /**
     * Interview Q: "When should you use platform threads vs virtual threads?"
     * 
     * Use Platform Threads:
     * - CPU-intensive tasks
     * - Pinning (synchronized, native code)
     * - Need precise control
     * 
     * Use Virtual Threads:
     * - I/O-bound tasks (HTTP, DB, file)
     * - High concurrency (10K+ tasks)
     * - Simplified code (no reactive, no callbacks)
     * 
     * Anti-pattern:
     * - DON'T pool virtual threads
     * - DON'T use for CPU-bound work
     */
}

/**
 * Structured Concurrency (Preview in Java 21)
 */
class StructuredConcurrencyDemo {
    
    public Result processOrder(String orderId) throws Exception {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            Future<User> user = scope.fork(() -> fetchUser(orderId));
            Future<Inventory> inventory = scope.fork(() -> checkInventory(orderId));
            Future<Payment> payment = scope.fork(() -> processPayment(orderId));
            
            scope.join();           // Wait for all
            scope.throwIfFailed();  // Propagate exceptions
            
            return new Result(user.resultNow(), inventory.resultNow(), payment.resultNow());
        }
        
        /**
         * Benefits:
         * - All subtasks complete or fail together
         * - Automatic cancellation if one fails
         * - Clear parent-child relationship
         * - No thread leaks
         */
    }
    
    private User fetchUser(String orderId) { return new User(); }
    private Inventory checkInventory(String orderId) { return new Inventory(); }
    private Payment processPayment(String orderId) { return new Payment(); }
    
    static class Result {}
    static class User {}
    static class Inventory {}
    static class Payment {}
}
```

---

#### 💻 LeetCode-Style Concurrency Problems

**Solve These (in order):**

1. **Print in Order** (Easy) - Semaphore coordination
2. **FizzBuzz Multithreaded** (Medium) - Condition synchronization  
3. **Dining Philosophers** (Classic) - Deadlock prevention
4. **Bounded Blocking Queue** (Medium) - Producer-consumer
5. **Web Crawler Multithreaded** (Medium) - URL deduplication with concurrency

**Example Solution: Dining Philosophers**

```java
import java.util.concurrent.locks.*;

/**
 * Classic problem: 5 philosophers, 5 forks
 * Avoid deadlock where all grab left fork and wait for right
 */
class DiningPhilosophers {
    // Solution: Resource ordering (always acquire lower-numbered fork first)
    private final Lock[] forks = new ReentrantLock[5];
    
    public DiningPhilosophers() {
        for (int i = 0; i < 5; i++) {
            forks[i] = new ReentrantLock();
        }
    }
    
    public void wantsToEat(int philosopher,
                          Runnable pickLeftFork,
                          Runnable pickRightFork,
                          Runnable eat,
                          Runnable putLeftFork,
                          Runnable putRightFork) {
        
        int leftFork = philosopher;
        int rightFork = (philosopher + 1) % 5;
        
        // CRITICAL: Always acquire lower number first to prevent circular wait
        int firstFork = Math.min(leftFork, rightFork);
        int secondFork = Math.max(leftFork, rightFork);
        
        forks[firstFork].lock();
        forks[secondFork].lock();
        
        try {
            pickLeftFork.run();
            pickRightFork.run();
            eat.run();
            putLeftFork.run();
            putRightFork.run();
        } finally {
            forks[secondFork].unlock();
            forks[firstFork].unlock();
        }
    }
    
    /**
     * Alternative solutions:
     * 1. Limit diners (allow max 4 philosophers at table)
     * 2. Asymmetric solution (odd pick left first, even pick right first)
     * 3. Waiter solution (centralized resource manager)
     */
}
```

---

### 🎯 Final Hour: Interview Q&A Preparation

**Practice explaining these verbally:**

#### Critical Interview Questions

**1. "Explain the Java Memory Model"**
```
The JMM defines how threads interact through memory and provides guarantees about 
visibility and ordering. Key concepts:

- Happens-before relationship: Ensures actions in one thread are visible to another
- Memory barriers: volatile and synchronized create barriers preventing reordering
- Main memory vs thread cache: Threads work with local copies, need synchronization 
  to propagate changes

Example: If thread A writes to volatile variable V, then thread B reads V, all of A's 
writes before writing V are visible to B after reading V.
```

**2. "When would you use ConcurrentHashMap vs synchronizedMap?"**
```
ConcurrentHashMap:
- Read-heavy workloads (lock-free reads in Java 8+)
- High concurrency (fine-grained locking per bucket)
- Don't need consistent snapshot during iteration
- Better scalability (no global lock)

synchronizedMap:
- Need atomic compound operations across multiple entries
- Need consistent iterator view
- Low concurrency (simple locking acceptable)
- Wrapping legacy code

Example: Web cache → ConcurrentHashMap; Transaction log → synchronizedMap
```

**3. "Design a thread-safe Singleton"**
```java
// Best: Initialization-on-demand holder
class Singleton {
    private Singleton() {}
    
    private static class Holder {
        static final Singleton INSTANCE = new Singleton();
    }
    
    public static Singleton getInstance() {
        return Holder.INSTANCE;
    }
}

Why it works:
- JVM guarantees thread-safe class initialization
- Lazy: Holder only loaded when getInstance() called
- No synchronization overhead
- No double-checked locking complexity
```

**4. "How do you prevent deadlocks?"**
```
1. Lock ordering: Always acquire locks in same global order
2. Lock timeout: Use tryLock() with timeout
3. Avoid nested locks: Minimize lock scope
4. Use higher-level constructs: ConcurrentHashMap, BlockingQueue

Example:
BAD:  Thread1: lock(A) → lock(B)
      Thread2: lock(B) → lock(A)  // Deadlock!

GOOD: Both threads: lock(min(A,B)) → lock(max(A,B))
```

**5. "Explain CompletableFuture vs Future"**
```
Future:
- Basic async result
- Blocking get()
- No composition

CompletableFuture:
- Non-blocking (thenApply, thenAccept)
- Composition (thenCompose, thenCombine)
- Exception handling (exceptionally, handle)
- Manual completion (complete, completeExceptionally)

Example:
CompletableFuture.supplyAsync(() -> fetchUser())
    .thenCompose(user -> fetchOrders(user))
    .thenApply(orders -> calculateTotal(orders))
    .exceptionally(ex -> 0.0)
    .thenAccept(total -> System.out.println(total));
```

---

## ✅ 2-Day Completion Checklist

### Day 1 - Internals Mastery
- [ ] Watched Defog Tech Java Memory Model videos
- [ ] Read JSR-133 FAQ (happens-before section)
- [ ] Understand volatile vs synchronized at memory level
- [ ] Can explain double-checked locking issue and fix
- [ ] Watched ThreadPoolExecutor internals (Pogrebinsky)
- [ ] Understand ConcurrentHashMap design (Java 8+)
- [ ] Coded lock-free stack using CAS
- [ ] Can compare synchronized/ReentrantLock/StampedLock

### Day 2 - Design & Modern Java
- [ ] Designed thread-safe TTL cache
- [ ] Implemented token bucket rate limiter
- [ ] Built priority blocking queue
- [ ] Understand Virtual Threads use cases
- [ ] Tried Structured Concurrency example
- [ ] Solved Dining Philosophers
- [ ] Solved 3+ LeetCode concurrency problems
- [ ] Can answer top 5 interview questions

---

## 📚 Quick Reference Card

### When to Use What

| Scenario | Solution | Why |
|----------|----------|-----|
| Simple flag shared between threads | `volatile boolean` | Visibility without atomicity |
| Counter incremented by multiple threads | `AtomicInteger` | Lock-free atomicity |
| Protect critical section | `synchronized` | Simplest, JVM-optimized |
| Need tryLock/timed lock | `ReentrantLock` | More features than synchronized |
| Read-heavy, rare writes | `StampedLock` optimistic | Lock-free reads |
| Many concurrent readers, few writers | `ReadWriteLock` | Multiple readers allowed |
| Thread-safe map | `ConcurrentHashMap` | Fine-grained locking |
| Bounded producer-consumer | `ArrayBlockingQueue` | Built-in blocking |
| Coordinate thread completion | `CountDownLatch` | One-time barrier |
| Limit concurrent access | `Semaphore` | Resource pool control |
| Async composition | `CompletableFuture` | Non-blocking pipeline |
| 10K+ I/O tasks | Virtual Threads | Lightweight concurrency |

---

## 🎯 Post-2 Days: What's Next

**If you have more time:**

1. **Read "Java Concurrency in Practice"** (Brian Goetz)
   - Bible of Java concurrency
   - Chapters 1-5 essential for interviews

2. **Study Real Systems**
   - Kafka producer internals (buffering, batching)
   - Netty event loop model
   - HikariCP connection pool

3. **Advanced Topics**
   - Memory fences and barriers
   - False sharing and cache line padding
   - Lock-free algorithms (Treiber stack, Michael-Scott queue)
   - Software Transactional Memory

4. **Practice System Design**
   - Design distributed rate limiter
   - Design task scheduler (cron-like)
   - Design real-time leaderboard

---

## 🔗 Essential Resources

### Must-Read
- **JSR-133 FAQ**: https://www.cs.umd.edu/~pugh/java/memoryModel/jsr-133-faq.html
- **Doug Lea's Site**: http://gee.cs.oswego.edu/dl/cpj/

### Video Courses (Pick One)
- **Michael Pogrebinsky (Udemy)**: Java Multithreading & Performance ⭐ Best value
- **Pluralsight**: Advanced Java Concurrent Patterns (Jose Paumard) ⭐ Most advanced

### YouTube Channels
- **Defog Tech**: Java concurrency, distributed systems ⭐ Highly recommended
- **Jakob Jenkov**: Concurrency tutorials (text + video)

### Practice
- **LeetCode**: Concurrency tag (15 problems)
- **InterviewBit**: Multithreading questions

---

## 💡 Final Tips for Interviews

1. **Always discuss trade-offs**
   - "ConcurrentHashMap is faster but doesn't guarantee consistent iteration"
   - "Virtual threads are great for I/O but not for CPU-bound work"

2. **Show you understand internals**
   - Don't just say "use ConcurrentHashMap"
   - Explain "ConcurrentHashMap uses CAS for lock-free reads and per-node locking for writes"

3. **Connect to production experience**
   - "I've seen deadlocks in production when..."
   - "We switched from thread pools to virtual threads because..."

4. **Ask clarifying questions**
   - "How many concurrent users?" (affects thread pool sizing)
   - "Read-heavy or write-heavy?" (affects lock choice)
   - "Can we use external cache?" (Redis vs in-memory)

5. **Code clearly**
   - Always unlock in finally block
   - Handle InterruptedException properly
   - Comment why you chose each synchronization mechanism

---

**Good luck with your LLD interviews! 🚀**

*Remember: Senior engineers don't just write concurrent code - they understand why it works and when it breaks.*

---

*Last Updated: January 2026*  
*Focus: LLD Interviews for Senior Engineers (5-15 YOE)*
