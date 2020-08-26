package cj.netos.chasechain.cacher.service;

import cj.lns.chip.sos.cube.framework.ICube;
import cj.lns.chip.sos.cube.framework.IDocument;
import cj.lns.chip.sos.cube.framework.IQuery;
import cj.lns.chip.sos.cube.framework.TupleDocument;
import cj.netos.chasechain.cacher.AbstractService;
import cj.netos.chasechain.cacher.ITrafficCacherService;
import cj.netos.chasechain.cacher.ITrafficPoolService;
import cj.netos.chasechain.cacher.TrafficCacherPointer;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.net.CircuitException;
import org.bson.Document;

@CjService(name = "defaultTrafficCacherService")
public class DefaultTrafficCacherService extends AbstractService implements ITrafficCacherService {

    ITrafficPoolService trafficPoolService;

    @Override
    public TrafficCacherPointer getPointer(String pool) throws CircuitException {
        ICube cube = cube(pool);
        //在etl.work项目中的AbstractService类中已建索引为倒序
        String cjql = String.format("select {'tuple':'*'}.sort({'tuple.lastCacheTime':-1}).limit(1) from tuple %s %s where {}", TrafficCacherPointer._COL_NAME, TrafficCacherPointer.class.getName());
        IQuery<TrafficCacherPointer> query = cube.createQuery(cjql);
        IDocument<TrafficCacherPointer> document = query.getSingleResult();
        if (document == null) {
            TrafficCacherPointer pointer = new TrafficCacherPointer();
            pointer.setLastCacheTime(0L);
            cube.saveDoc(TrafficCacherPointer._COL_NAME, new TupleDocument<>(pointer));
            cube.createIndex(TrafficCacherPointer._COL_NAME, Document.parse(String.format("{'tuple.lastCacheTime':-1}")));
            return pointer;
        }
        return document.tuple();
    }

    @Override
    public void movePointer(String sourcePool, TrafficCacherPointer fromPointer, long endTime) throws CircuitException {
        long beginTime = fromPointer.getLastCacheTime();
        if (endTime < 1) {//如果为0表示当前时间
            endTime = System.currentTimeMillis();
        }
        if (beginTime == endTime) {//不动
            return;
        }
        ICube cube = cube(sourcePool);
        TrafficCacherPointer pointer = new TrafficCacherPointer();
        pointer.setLastCacheTime(endTime);
        cube.saveDoc(TrafficCacherPointer._COL_NAME, new TupleDocument<>(pointer));
    }


    @Override
    public void resetPool(String pool) throws CircuitException {
        ICube cube = cube(pool);
        cube.dropTuple(TrafficCacherPointer._COL_NAME);
    }

    @Override
    public void clearPointersExceptTop(String pool, int retains) throws CircuitException {
        ICube cube = cube(pool);
        //在etl.work项目中的AbstractService类中已建索引为倒序
        String cjql = String.format("select {'tuple':'*'}.sort({'tuple.lastCacheTime':-1}).limit(1).skip(%s) from tuple %s %s where {}", retains - 1, TrafficCacherPointer._COL_NAME, TrafficCacherPointer.class.getName());
        IQuery<TrafficCacherPointer> query = cube.createQuery(cjql);
        IDocument<TrafficCacherPointer> document = query.getSingleResult();
        if (document == null) {
            return;
        }
        TrafficCacherPointer pointer = document.tuple();
        cube.deleteDocs(TrafficCacherPointer._COL_NAME, String.format("{'tuple.lastCacheTime':{'$lt':%s}}", pointer.getLastCacheTime()));
    }
}
