public class ThreadsTest {
    public static void main(String[] args) {
        Thread thread = new Thread(() -> System.out.println(Thread.currentThread().getName()));
        thread.setName("MyFirstThread");

        System.out.println(Thread.currentThread().getName());
        thread.start();
        // thread.join();
        System.out.println(Thread.currentThread().getName());

    }
}
