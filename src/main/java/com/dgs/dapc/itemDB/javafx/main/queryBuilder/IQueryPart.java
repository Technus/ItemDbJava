package com.dgs.dapc.itemDB.javafx.main.queryBuilder;

import com.mongodb.QueryBuilder;

public interface IQueryPart {
    QueryBuilder getBuilder();
}
