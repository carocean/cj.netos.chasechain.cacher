package cj.netos.chasechain.cacher.service;

import cj.lns.chip.sos.cube.framework.ICube;
import cj.lns.chip.sos.cube.framework.IDocument;
import cj.lns.chip.sos.cube.framework.IQuery;
import cj.lns.chip.sos.cube.framework.TupleDocument;
import cj.netos.chasechain.cacher.AbstractService;
import cj.netos.chasechain.cacher.ITrafficCacherService;
import cj.netos.chasechain.cacher.ITrafficPoolService;
import cj.netos.chasechain.cacher.TrafficCacherPointer;
import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceSite;
import cj.studio.ecm.net.CircuitException;
import cj.ultimate.gson2.com.google.gson.Gson;
import cj.ultimate.util.StringUtil;
import org.bson.Document;

@CjService(name = "defaultTrafficCacherService")
public class DefaultTrafficCacherService extends AbstractService implements ITrafficCacherService {

    ITrafficPoolService trafficPoolService;
    @CjServiceSite
    IServiceSite site;

    @Override
    public TrafficCacherPointer getPointer(String pool) throws CircuitException {
        ICube cube = cube(pool);
        //在etl.work项目中的AbstractService类中已建索引为倒序
        String cjql = String.format("select {'tuple':'*'}.sort({'tuple.itemLastCacheTime':-1}).limit(1) from tuple %s %s where {}", TrafficCacherPointer._COL_NAME, TrafficCacherPointer.class.getName());
        IQuery<TrafficCacherPointer> query = cube.createQuery(cjql);
        IDocument<TrafficCacherPointer> document = query.getSingleResult();
        if (document == null) {
            String day = site.getProperty("traffic.cache.pointer.beginDay");
            if (StringUtil.isEmpty(day)) {
                day = "7";
            }
            CJSystem.logging().debug(getClass(), String.format("第一次缓冲，从当前时间之前的第%s天开始缓冲", day));
            TrafficCacherPointer pointer = new TrafficCacherPointer();
            long time = System.currentTimeMillis() - (Integer.valueOf(day) * 24 * 60 * 60 * 1000);//如果第一次缓冲，则只取一周之前的（7天）
            pointer.setItemLastCacheTime(time);
            pointer.setBehaviorLastCacheTime(time);
            cube.saveDoc(TrafficCacherPointer._COL_NAME, new TupleDocument<>(pointer));
            cube.createIndex(TrafficCacherPointer._COL_NAME, Document.parse(String.format("{'tuple.itemLastCacheTime':-1}")));
            return pointer;
        }
        return document.tuple();
    }

    @Override
    public void moveItemPointer(String sourcePool, TrafficCacherPointer fromPointer, long endTime) throws CircuitException {
        long beginTime = fromPointer.getItemLastCacheTime();
        if (endTime < 1) {//如果为0表示当前时间
            endTime = System.currentTimeMillis();
        }
        if (beginTime == endTime) {//不动
            return;
        }
        ICube cube = cube(sourcePool);

        cube.updateDocOne(TrafficCacherPointer._COL_NAME,
                Document.parse(String.format("{}")),
                Document.parse(String.format("{'$set':{'tuple.itemLastCacheTime':%s}}",
                        endTime)));
    }


    @Override
    public void moveBehaviorPointer(String sourcePool, TrafficCacherPointer fromPointer, long endTime) throws CircuitException {
        long beginTime = fromPointer.getItemLastCacheTime();
        if (endTime < 1) {//如果为0表示当前时间
            endTime = System.currentTimeMillis();
        }
        if (beginTime == endTime) {//不动
            return;
        }
        ICube cube = cube(sourcePool);

        cube.updateDocOne(TrafficCacherPointer._COL_NAME,
                Document.parse(String.format("{}")),
                Document.parse(String.format("{'$set':{'tuple.behaviorLastCacheTime':%s}}",
                        endTime)));
    }

    @Override
    public void resetPool(String pool) throws CircuitException {
        ICube cube = cube(pool);
        cube.dropTuple(TrafficCacherPointer._COL_NAME);
    }
}
