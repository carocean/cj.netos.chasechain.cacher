package cj.netos.chasechain.cacher.service;

import cj.lns.chip.sos.cube.framework.ICube;
import cj.lns.chip.sos.cube.framework.IDocument;
import cj.lns.chip.sos.cube.framework.IQuery;
import cj.lns.chip.sos.cube.framework.TupleDocument;
import cj.netos.chasechain.cacher.*;
import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.annotation.CjServiceSite;
import cj.studio.ecm.net.CircuitException;
import redis.clients.jedis.JedisCluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CjService(name = "defaultTrafficPoolService")
public class DefaultTrafficPoolService implements ITrafficPoolService, Constants {

    @CjServiceRef(refByName = "mongodb.netos.home")
    ICube home;
    @CjServiceRef(refByName = "@.redis.cluster")
    JedisCluster jedisCluster;
    @CjServiceRef(refByName = "defaultTrafficCacherService")
    ITrafficCacherService trafficCacherService;
    @CjServiceRef(refByName = "defaultContentItemService")
    IContentItemService contentItemService;
    Map<Integer, LevelCacheSize> levelCacheSizeMap;
    @CjServiceSite
    IServiceSite site;

    @Override
    public TrafficPool getTrafficPool(String trafficPool) {
        String cjql = String.format("select {'tuple':'*'} from tuple %s %s where {'tuple.id':'%s'}", TrafficPool._COL_NAME, TrafficPool.class.getName(), trafficPool);
        IQuery<TrafficPool> query = home.createQuery(cjql);
        IDocument<TrafficPool> document = query.getSingleResult();
        if (document == null) {
            return null;
        }
        return document.tuple();
    }

    @Override
    public TrafficPool getCountryTrafficPool() {
        String cjql = String.format("select {'tuple':'*'} from tuple %s %s where {'tuple.level':0}", TrafficPool._COL_NAME, TrafficPool.class.getName());
        IQuery<TrafficPool> query = home.createQuery(cjql);
        IDocument<TrafficPool> document = query.getSingleResult();
        if (document == null) {
            return null;
        }
        return document.tuple();
    }

    @Override
    public List<TrafficPool> listChildTrafficPools(String trafficPool) {
        String cjql = String.format("select {'tuple':'*'} from tuple %s %s where {'tuple.parent':'%s'}", TrafficPool._COL_NAME, TrafficPool.class.getName(), trafficPool);
        IQuery<TrafficPool> query = home.createQuery(cjql);
        List<IDocument<TrafficPool>> list = query.getResultList();
        List<TrafficPool> pools = new ArrayList<>();
        for (IDocument<TrafficPool> document : list) {
            pools.add(document.tuple());
        }
        return pools;
    }

    @Override
    public List<TrafficPool> pageTrafficPool(int limit, long offset) {
        String cjql = String.format("select {'tuple':'*'}.limit(%s).skip(%s) from tuple %s %s where {}", limit, offset, TrafficPool._COL_NAME, TrafficPool.class.getName());
        IQuery<TrafficPool> query = home.createQuery(cjql);
        List<IDocument<TrafficPool>> list = query.getResultList();
        List<TrafficPool> pools = new ArrayList<>();
        for (IDocument<TrafficPool> document : list) {
            pools.add(document.tuple());
        }
        return pools;
    }

    @Override
    public void cache(TrafficPool pool) throws CircuitException {
        TrafficCacherPointer pointer = trafficCacherService.getPointer(pool.getId());
        _cacheOnNewItems(pool, pointer);
        _cacheOnBehaviorItems(pool, pointer);
    }


    private void _cacheOnNewItems(TrafficPool pool, TrafficCacherPointer pointer) throws CircuitException {
        LevelCacheSize levelCacheSize = levelCacheSizeMap.get(pool.getLevel());//1级池缓冲多少；2级多少；常规多少等等
        int cacheSize = 10000;
        if (levelCacheSize != null) {
            cacheSize = levelCacheSize.getCapacity();
        }
        CJSystem.logging().info(getClass(), String.format("\t%s的缓冲区大小:%s", pool.getTitle(), cacheSize));
        int limit = 100;
        long offset = 0;
        long endTime = 0;
        try {
            //目的是缓冲一个cacheSize > offset的区间的物品，并且以物品的上次创建时间为基点，即在池中有新的来就缓冲它
            while (cacheSize > offset) {
                List<ContentItem> items = contentItemService.pageContentItem(pool.getId(), pointer, limit, offset);
                if (items.isEmpty()) {
                    break;
                }
                offset += items.size();
                for (ContentItem item : items) {
                    if (item.getCtime() > endTime) {
                        endTime = item.getCtime();
                    }
                    //缓冲内容物
                    String key = String.format("%s.%s", redis_cacher_pool_item_key, pool.getId());
                    jedisCluster.sadd(key, item.getId());
                }
            }
        } finally {
            trafficCacherService.moveItemPointer(pool.getId(), pointer, endTime);
            CJSystem.logging().info(getClass(), String.format("\t按内容物的创建时间缓冲完成:%s，实际缓冲了 %s 个。", pool.getTitle(), offset));
        }

    }

    private void _cacheOnBehaviorItems(TrafficPool pool, TrafficCacherPointer pointer) throws CircuitException {
        int limit = 100;
        long offset = 0;
        long endTime = 0;
        //实际上不可能缓冲所有物品，因此缓冲的策略应用：1。有新物品则缓冲；2。已有物品的行为有更新则缓冲它（不按活跃度）
        try {
            while (true) {
                List<ItemBehavior> items = contentItemService.pageBehavior(pool.getId(), pointer, limit, offset);
                if (items.isEmpty()) {
                    break;
                }
                offset += items.size();
                for (ItemBehavior itemBehavior : items) {
                    if (itemBehavior.getUtime() > endTime) {
                        endTime = itemBehavior.getUtime();
                    }
                    //缓冲内容物
                    String key = String.format("%s.%s", redis_cacher_pool_item_key, pool.getId());
                    jedisCluster.sadd(key, itemBehavior.getItem());
                }
            }
        } finally {
            trafficCacherService.moveBehaviorPointer(pool.getId(), pointer, endTime);
            CJSystem.logging().info(getClass(), String.format("\t按内容物行为变动时间缓冲完成:%s，实际缓冲了 %s 个。", pool.getTitle(), offset));
        }

    }

    @Override
    public void resetAllPoolCachePointer() throws CircuitException {
        int limit = 100;
        long offset = 0;
        while (true) {
            List<TrafficPool> pools = pageTrafficPool(limit, offset);
            if (pools.isEmpty()) {
                break;
            }
            offset += pools.size();
            for (TrafficPool pool : pools) {
                trafficCacherService.resetPool(pool.getId());
                String key = String.format("%s.%s", redis_cacher_pool_item_key, pool.getId());
                jedisCluster.del(key);
                CJSystem.logging().info(getClass(), String.format("已重置池:%s[%s]", pool.getTitle(), pool.getId()));
            }
        }
    }

    @Override
    public void configCacheSize(List<LevelCacheSize> levelCacheSize) {
        if (levelCacheSizeMap == null) {
            levelCacheSizeMap = new HashMap<>();
        } else {
            levelCacheSizeMap.clear();
        }
        home.dropTuple("chasechain.cacher.config");
        for (LevelCacheSize size : levelCacheSize) {
            home.saveDoc("chasechain.cacher.config", new TupleDocument<>(size));
            levelCacheSizeMap.put(size.getLevel(), size);
        }
    }

    @Override
    public void loadCacheSizeConfig() {
        if (levelCacheSizeMap == null) {
            levelCacheSizeMap = new HashMap<>();
        } else {
            levelCacheSizeMap.clear();
        }
        String cjql = String.format("select {'tuple':'*'} from tuple chasechain.cacher.config %s where {}", LevelCacheSize.class.getName());
        IQuery<LevelCacheSize> query = home.createQuery(cjql);
        List<IDocument<LevelCacheSize>> documents = query.getResultList();
        for (IDocument<LevelCacheSize> document : documents) {
            LevelCacheSize size = document.tuple();
            levelCacheSizeMap.put(size.getLevel(), size);
        }
    }
}
