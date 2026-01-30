Defog Tech YouTube notes
https://youtu.be/WH5UvQJizH0?si=1BeBqoGyup7C9ukI

### volatile vs atomic
a variable's value is copied by all the threads and the copy is used from local cache
so any update to the variable value is not propagated

#### volatile (solves visibility problem)
volatile keyword forces the threads to get the updated value

change to local cache -> shared cache -> refreshed to other cores where other threads are running

### synchronization problem
```java
volatile int value = 1;
value++;
```

even with volatile there is no guarantee which thread will update.

to solve we use synchronize
```java
volatile int value = 1;
synchronized(obj) {
    // only one thread will come into this block and do the compound operation
    value++;
}
```

or use AtomicInteger for compound operations
single operation that does the update
```java
AtomicInteger value = new AtomicInteger(1);
value.increment();

// other methods
incrementAndGet
decrementAndGet
addAndGet(int delta)
compareAndSet(int expectedValue, int newValue)
```


### Thread Pool
let's say we want to create 1000 threads each creating heavy objects is very impractical
so we create a fixed pool of threads to do the job, thread finishes and returns to the pool and reused.

```java
// creates only 10 threads in the pool to be reused
private static ExecutorService threadPool = Executors.newFixedThreadPool(10); 

public static void main(String[] args) throws InterruptedException {
    for (int i = 0; i < 1000; i++) {
        int id = i;
        threadPool.submit() -> {
            String birthDate = new UserService().birthDate(id);
            System.out.println(birthDate);
        });
    }
    Thread.sleep(1000);
}
```
