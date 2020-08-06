package cj.netos.chasechain.cacher.ports;

import cj.netos.chasechain.cacher.*;
import cj.studio.ecm.CJSystem;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.net.CircuitException;
import cj.studio.openport.ISecuritySession;

import java.util.List;

@CjService(name = "/engine.ports")
public class DefaultCacherPorts implements ICacherPorts {
    @CjServiceRef(refByName = "defaultCacherEngine")
    ICacherEngine cacherEngine;
    @CjServiceRef(refByName = "defaultTrafficPoolService")
    ITrafficPoolService trafficPoolService;

    //按源数据表的ctime作为增量抽取的依据
    @Override
    public boolean isRunning(ISecuritySession securitySession) throws CircuitException {
        return cacherEngine.isRunning();
    }

    @Override
    public void start(ISecuritySession securitySession, int workThreadCount, long delay, long period) throws CircuitException {
        if (!securitySession.roleIn("platform:administrators")) {
            throw new CircuitException("801", "拒绝访问");
        }
        trafficPoolService.loadCacheSizeConfig();
        cacherEngine.start(securitySession.principal(), workThreadCount, delay, period);
    }

    @Override
    public void stop(ISecuritySession securitySession) throws CircuitException {
        if (!securitySession.roleIn("platform:administrators")) {
            throw new CircuitException("801", "拒绝访问");
        }
        cacherEngine.stop();
    }

    @Override
    public void resetAllCacherPoint(ISecuritySession securitySession) throws CircuitException {
        if (!securitySession.roleIn("platform:administrators")) {
            throw new CircuitException("801", "拒绝访问");
        }
        CJSystem.logging().info(getClass(), String.format("开始重置缓冲指针..."));
        trafficPoolService.resetAllPoolCachePointer();
        CJSystem.logging().info(getClass(), String.format("重完缓冲指针完成!"));
    }

    @Override
    public void configCacheSize(ISecuritySession securitySession, List<LevelCacheSize> levelCacheSize) throws CircuitException {
        trafficPoolService.configCacheSize(levelCacheSize);
    }
}
