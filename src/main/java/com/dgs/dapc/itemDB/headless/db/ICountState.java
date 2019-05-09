package com.dgs.dapc.itemDB.headless.db;

import javafx.beans.property.ReadOnlyDoubleProperty;

public interface ICountState {
    Double getCount();
    ReadOnlyDoubleProperty countProperty();
}
