package com.dgs.dapc.itemDB.javafx.main.queryBuilder;

import com.mongodb.QueryBuilder;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.TreeItem;

public class QueryGroup implements IQueryPart {
    private final TreeItem<IQueryPart> treeItem =new TreeItem<>();
    private final SimpleBooleanProperty orLogic =new SimpleBooleanProperty(false);

    public TreeItem<IQueryPart> getTreeItem() {
        return treeItem;
    }

    public boolean isOrLogic() {
        return orLogic.get();
    }

    public SimpleBooleanProperty orLogicProperty() {
        return orLogic;
    }

    public void setOrLogic(boolean orLogic) {
        this.orLogic.set(orLogic);
    }

    public QueryBuilder getBuilder(){
        QueryBuilder builder=QueryBuilder.start();
        if(isOrLogic()){
            treeItem.getChildren().forEach(sub-> getBuilder().or(sub.getValue().getBuilder().get()));
        }else {
            treeItem.getChildren().forEach(sub-> getBuilder().and(sub.getValue().getBuilder().get()));

        }
        return builder;
    }
}
