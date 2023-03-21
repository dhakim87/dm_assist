package com.dm.assist.common;

public interface AsyncHook<T> {
    void onPostExecute(T val);
}
