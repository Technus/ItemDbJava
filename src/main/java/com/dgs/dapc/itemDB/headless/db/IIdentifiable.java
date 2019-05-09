package com.dgs.dapc.itemDB.headless.db;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.bson.types.ObjectId;

public interface IIdentifiable {
    DiscriminatedObjectId getDiscriminatedId();
    ReadOnlyObjectProperty<DiscriminatedObjectId> discriminatedIdProperty();
    ObjectId getId();
    void setId(ObjectId id);
    SimpleObjectProperty<ObjectId> idProperty();
}
