import execute.Executor;

public class Toge {
    private Toge() {
    }

    public static void main(String[] args) {
        Executor executor = new Executor(args);
        executor.execute();
    }
}
