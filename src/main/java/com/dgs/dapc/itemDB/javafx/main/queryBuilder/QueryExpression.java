package com.dgs.dapc.itemDB.javafx.main.queryBuilder;

import com.mongodb.QueryBuilder;

public class QueryExpression implements IQueryPart {
    public QueryBuilder getBuilder(){
        QueryBuilder builder=QueryBuilder.start();

        return builder;
    }
}
