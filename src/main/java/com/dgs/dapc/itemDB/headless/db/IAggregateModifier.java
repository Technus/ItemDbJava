package com.dgs.dapc.itemDB.headless.db;

import org.bson.conversions.Bson;

import java.util.List;

public interface IAggregateModifier {
    void modifyAggregate(List<Bson> aggregate);
}
