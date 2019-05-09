package com.dgs.dapc.itemDB.headless.db;

import javafx.beans.property.SimpleDoubleProperty;

public interface IStock extends IStockState,ICount {
    void setOrdered(Double ordered);
    void setMinCount(Double minCount);
    SimpleDoubleProperty orderedProperty();
    SimpleDoubleProperty minCountProperty();
}
