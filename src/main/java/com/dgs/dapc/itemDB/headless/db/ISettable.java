package com.dgs.dapc.itemDB.headless.db;

public interface ISettable<T> {
    void setData(T value);
    void setFully(T value);
}
