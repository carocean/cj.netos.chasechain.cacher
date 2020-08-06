package cj.netos.chasechain.cacher;

public class TrafficCacherPointer {
    public static transient final String _COL_NAME = "traffic.cache.pointers";
    long lastCacheTime;

    public long getLastCacheTime() {
        return lastCacheTime;
    }

    public void setLastCacheTime(long lastCacheTime) {
        this.lastCacheTime = lastCacheTime;
    }
}
