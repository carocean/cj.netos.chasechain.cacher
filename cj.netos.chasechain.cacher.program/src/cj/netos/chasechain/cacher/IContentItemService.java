package cj.netos.chasechain.cacher;

import cj.studio.ecm.net.CircuitException;

import java.util.List;

public interface IContentItemService {

    List<ContentItem> pageContentItem(String trafficPool, TrafficCacherPointer pointer, int limit, long offset) throws CircuitException;

    List<ItemBehavior> pageBehavior(String trafficPool, TrafficCacherPointer pointer, int limit, long offset) throws CircuitException;
}
