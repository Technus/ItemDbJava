package com.dgs.dapc.itemDB.headless.db;

import javafx.beans.property.SimpleBooleanProperty;

public interface IExists {
    SimpleBooleanProperty existsProperty();
    boolean getExists();
    void setExists(boolean exists);
}
