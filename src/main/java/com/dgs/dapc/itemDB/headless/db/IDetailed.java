package com.dgs.dapc.itemDB.headless.db;

import javafx.beans.property.SimpleStringProperty;

public interface IDetailed {
    String getDetails();
    void setDetails(String details);
    SimpleStringProperty detailsProperty();
}
