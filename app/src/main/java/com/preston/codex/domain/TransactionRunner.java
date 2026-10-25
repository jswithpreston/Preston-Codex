package com.preston.codex.domain;

import java.util.function.Supplier;

/**
 * Domain transaction runner interface decoupling transaction demarcation
 * from specific persistence mechanisms. Pure Java, no android.* dependencies.
 */
public interface TransactionRunner {
    <T> T runInTransaction(Supplier<T> action);
    void runInTransaction(Runnable action);
}
