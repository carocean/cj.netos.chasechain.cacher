package cj.netos.chasechain.cacher;

public interface ICacherEngine {
    boolean isRunning();

    void stop();

    void start(String operator, int workThreadCount, long delay, long period);

}
