package com.dgs.dapc.itemDB.headless.db;

import javafx.beans.property.ReadOnlyStringProperty;

public interface ISerialState {
    String getSerial();
    ReadOnlyStringProperty serialProperty();
}
