package com.dgs.dapc.itemDB.headless.db;

import javafx.beans.property.SimpleStringProperty;

public interface INamed {
    String getName();
    void setName(String name);
    SimpleStringProperty nameProperty();
}
