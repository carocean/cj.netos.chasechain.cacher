package cj.netos.chasechain.cacher;

public class ContentItem {
    public final static String _COL_NAME = "content.items";
    String id;
    long ctime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getCtime() {
        return ctime;
    }

    public void setCtime(long ctime) {
        this.ctime = ctime;
    }
}
