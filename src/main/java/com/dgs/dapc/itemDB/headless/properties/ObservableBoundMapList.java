package com.dgs.dapc.itemDB.headless.properties;

import com.dgs.dapc.itemDB.headless.db.IIdentifiable;
import com.mongodb.annotations.NotThreadSafe;
import com.sun.javafx.collections.ObservableMapWrapper;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import org.bson.types.ObjectId;

import java.util.LinkedHashMap;
import java.util.function.Function;

@NotThreadSafe
public class ObservableBoundMapList<V extends IIdentifiable>{
    private Function<ObjectId,V> query=objectId -> null;
    private Function<ObjectId,V> creator=objectId -> null;
    public final ObservableReadSortList<V> readableAndSortableList = new ObservableReadSortList<>(FXCollections.observableArrayList());
    public final ObservableMapWrapper<ObjectId,V> map = new ObservableMapWrapper<>(new LinkedHashMap<>());
    {
        map.addListener((MapChangeListener<ObjectId, V>) change -> {
            if(change.wasRemoved()){
                readableAndSortableList.backingList.remove(change.getValueRemoved());
            }
            if(change.wasAdded()){
                readableAndSortableList.backingList.add(change.getValueAdded());
            }
        });
    }

    public ObservableBoundMapList(){}

    public ObservableBoundMapList(Function<ObjectId,V> creator){
        this.creator = creator;
    }

    public Function<ObjectId, V> getQuery() {
        return query;
    }

    public void setQuery(Function<ObjectId, V> query) {
        this.query = query;
    }

    public Function<ObjectId, V> getCreator() {
        return creator;
    }

    public void setCreator(Function<ObjectId, V> creator) {
        this.creator = creator;
    }

    public V getAndMakeIfMissing(ObjectId id){
        V v=map.get(id);
        if(v==null){
            v=query.apply(id);
            if(v==null){
                v=creator.apply(id);
            }
            if(v!=null) {
                map.put(id, v);
            }
        }
        return v;
    }
}