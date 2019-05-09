package com.dgs.dapc.itemDB.headless.db;

import com.dgs.dapc.itemDB.headless.properties.NamedUrlProperty;

public interface ILinked {
    String getUrl();
    void setUrl(String url);
    NamedUrlProperty urlProperty();
}
