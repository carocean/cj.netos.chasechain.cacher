package cj.netos.chasechain.cacher.service;

import cj.lns.chip.sos.cube.framework.ICube;
import cj.lns.chip.sos.cube.framework.IDocument;
import cj.lns.chip.sos.cube.framework.IQuery;
import cj.netos.chasechain.cacher.AbstractService;
import cj.netos.chasechain.cacher.ContentItem;
import cj.netos.chasechain.cacher.IContentItemService;
import cj.netos.chasechain.cacher.TrafficCacherPointer;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.net.CircuitException;

import java.util.ArrayList;
import java.util.List;

@CjService(name = "defaultContentItemService")
public class DefaultContentItemService extends AbstractService implements IContentItemService {
    @Override
    public List<ContentItem> pageContentItem(String trafficPool, TrafficCacherPointer pointer, int limit, long offset) throws CircuitException {
        ICube cube = cube(trafficPool);
        String cjql = String.format("select {'tuple.id':1,'tuple.ctime':1}.sort({'tuple.ctime':-1}).limit(%s).skip(%s) from tuple %s %s where {'tuple.ctime':{'$gt':%s}}",
                limit, offset, ContentItem._COL_NAME, ContentItem.class.getName(), pointer.getLastCacheTime());
        IQuery<ContentItem> query = cube.createQuery(cjql);
        List<IDocument<ContentItem>> list = query.getResultList();
        List<ContentItem> contentItems = new ArrayList<>();
        for (IDocument<ContentItem> document : list) {
            contentItems.add(document.tuple());
        }
        return contentItems;
    }
}
