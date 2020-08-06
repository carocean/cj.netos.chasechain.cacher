package cj.netos.chasechain.cacher;

public class CacherEvent {
    String key;
    Object args;
    public CacherEvent(String key) {
        this.key = key;
    }
    public CacherEvent(String key, Object args) {
        this.key = key;
        this.args=args;
    }

    public Object getArgs() {
        return args;
    }

    public void setArgs(Object args) {
        this.args = args;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
