package cj.netos.chasechain.cacher.cmd;

import cj.netos.chasechain.cacher.*;
import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.net.CircuitException;

import java.util.List;

public class TrafficPoolCommand implements ICacherCommand {

    ITrafficPoolService trafficPoolService;
    ICacherEngine bubblerEngine;

    public TrafficPoolCommand(IServiceSite site) {
        trafficPoolService = (ITrafficPoolService) site.getService("defaultTrafficPoolService");
        bubblerEngine = (ICacherEngine) site.getService("defaultCacherEngine");
    }

    @Override
    public void doCommand(CacherEvent cacherEvent) throws CircuitException {
        int limit = 100;
        long offset = 0;
        while (bubblerEngine.isRunning()) {
            List<TrafficPool> pools = trafficPoolService.pageTrafficPool(limit, offset);
            if (pools.isEmpty()) {
                break;
            }
            offset += pools.size();
            for (TrafficPool pool : pools) {
                synchronized (pool.getId()) {
                    CJSystem.logging().info(getClass(), String.format("正在缓冲池:%s[%s] ...", pool.getTitle(), pool.getId()));
                    trafficPoolService.cache(pool);
                    CJSystem.logging().info(getClass(), String.format("完成缓冲池:%s[%s]", pool.getTitle(), pool.getId()));
                }
            }
        }
    }
}
