package com.dm.assist.common;

public interface Observable<T> {
    void onChange(T val);
}
