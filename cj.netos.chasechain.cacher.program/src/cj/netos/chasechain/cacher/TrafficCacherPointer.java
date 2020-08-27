package cj.netos.chasechain.cacher;

public class TrafficCacherPointer {
    public static transient final String _COL_NAME = "traffic.cache.pointers";
    long itemLastCacheTime;
    long behaviorLastCacheTime;

    public long getItemLastCacheTime() {
        return itemLastCacheTime;
    }

    public void setItemLastCacheTime(long itemLastCacheTime) {
        this.itemLastCacheTime = itemLastCacheTime;
    }

    public long getBehaviorLastCacheTime() {
        return behaviorLastCacheTime;
    }

    public void setBehaviorLastCacheTime(long behaviorLastCacheTime) {
        this.behaviorLastCacheTime = behaviorLastCacheTime;
    }
}
