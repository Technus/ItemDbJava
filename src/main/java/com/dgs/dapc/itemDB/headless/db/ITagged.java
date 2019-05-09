package com.dgs.dapc.itemDB.headless.db;

import javafx.beans.property.ReadOnlyStringProperty;

public interface ITagged {
    String getTagsString();
    ReadOnlyStringProperty tagsStringProperty();
}
