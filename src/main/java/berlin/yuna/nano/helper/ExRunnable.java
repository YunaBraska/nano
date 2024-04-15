package berlin.yuna.nano.helper;

@FunctionalInterface
public interface ExRunnable {
    @SuppressWarnings({"java:S112", "RedundantThrows"})
    void run() throws Exception;
}
