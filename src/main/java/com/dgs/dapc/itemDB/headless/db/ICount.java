package com.dgs.dapc.itemDB.headless.db;

import javafx.beans.property.SimpleDoubleProperty;

public interface ICount extends ICountState {
    void setCount(Double count);
    SimpleDoubleProperty countProperty();
}
