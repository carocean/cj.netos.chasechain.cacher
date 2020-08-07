package cj.netos.chasechain.cacher.service;

import cj.lns.chip.sos.cube.framework.ICube;
import cj.lns.chip.sos.cube.framework.IDocument;
import cj.lns.chip.sos.cube.framework.IQuery;
import cj.lns.chip.sos.cube.framework.TupleDocument;
import cj.netos.chasechain.cacher.*;
import cj.studio.ecm.CJSystem;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
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
        LevelCacheSize levelCacheSize = levelCacheSizeMap.get(pool.getLevel());//1级池缓冲多少；2级多少；常规多少等等
        int cacheSize = 10000;
        if (levelCacheSize != null) {
            cacheSize = levelCacheSize.getCapacity();
        }
        CJSystem.logging().info(getClass(), String.format("流量池：%s[%s]的缓冲区大小是:%s", pool.getTitle(), pool.getId(), cacheSize));
        int limit = 100;
        long offset = 0;
        long endTime = 0;
        TrafficCacherPointer pointer = trafficCacherService.getPointer(pool.getId());
        try {
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
                    String key = String.format("%s.%s",redis_cacher_pool_item_key,pool.getId());
                    jedisCluster.sadd(key, item.getId());
                }
            }
        } finally {
            trafficCacherService.movePointer(pool.getId(), pointer, endTime);
            CJSystem.logging().info(getClass(), String.format("流量池缓冲完成:%s[%s]，实际缓冲了 %s 个。", pool.getTitle(), pool.getId(), offset));
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
                String key = String.format("%s.%s",redis_cacher_pool_item_key,pool.getId());
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
