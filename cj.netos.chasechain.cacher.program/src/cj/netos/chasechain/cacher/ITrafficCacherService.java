package cj.netos.chasechain.cacher;

import cj.studio.ecm.net.CircuitException;

public interface ITrafficCacherService {


    TrafficCacherPointer getPointer(String pool) throws CircuitException;

    void moveItemPointer(String pool, TrafficCacherPointer fromPointer, long lastItemTime) throws CircuitException;


    void moveBehaviorPointer(String sourcePool, TrafficCacherPointer fromPointer, long endTime) throws CircuitException;

    void resetPool(String id) throws CircuitException;


}
