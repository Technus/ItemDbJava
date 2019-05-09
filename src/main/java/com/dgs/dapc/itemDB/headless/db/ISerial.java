package com.dgs.dapc.itemDB.headless.db;

import javafx.beans.property.SimpleStringProperty;

public interface ISerial extends ISerialState {
    void setSerial(String serial);
    SimpleStringProperty serialProperty();
}
