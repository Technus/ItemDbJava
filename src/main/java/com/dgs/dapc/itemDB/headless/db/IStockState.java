package com.dgs.dapc.itemDB.headless.db;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;

public interface IStockState extends ICountState {
    Double getOrdered();
    Double getMinCount();
    boolean isStockOptimal();
    ReadOnlyDoubleProperty orderedProperty();
    ReadOnlyDoubleProperty minCountProperty();
    ReadOnlyBooleanProperty stockOptimalProperty();
}
