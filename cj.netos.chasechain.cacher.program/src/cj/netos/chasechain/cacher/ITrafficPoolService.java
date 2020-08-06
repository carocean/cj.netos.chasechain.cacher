package cj.netos.chasechain.cacher;

import cj.studio.ecm.net.CircuitException;

import java.util.List;

public interface ITrafficPoolService {

    TrafficPool getTrafficPool(String trafficPool);
    TrafficPool getCountryTrafficPool();
    List<TrafficPool> listChildTrafficPools(String trafficPool);

    List<TrafficPool> pageTrafficPool(int limit, long offset);

    void cache(TrafficPool pool) throws CircuitException;

    void resetAllPoolCachePointer() throws CircuitException;

    void configCacheSize(List<LevelCacheSize> levelCacheSize);

    void loadCacheSizeConfig();
}
