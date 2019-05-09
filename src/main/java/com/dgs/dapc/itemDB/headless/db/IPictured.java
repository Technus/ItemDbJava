package com.dgs.dapc.itemDB.headless.db;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.image.Image;

public interface IPictured {
    String getPicture();
    void setPicture(String pictureLink);
    SimpleStringProperty pictureProperty();
    ReadOnlyObjectProperty<Image> imageProperty();
    ReadOnlyBooleanProperty containingImageProperty();
}
