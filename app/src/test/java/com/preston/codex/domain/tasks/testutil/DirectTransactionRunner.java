package com.preston.codex.domain.tasks.testutil;

import com.preston.codex.domain.TransactionRunner;

import java.util.function.Supplier;

public class DirectTransactionRunner implements TransactionRunner {
    @Override
    public void runInTransaction(Runnable runnable) {
        runnable.run();
    }

    @Override
    public <T> T runInTransaction(Supplier<T> action) {
        return action.get();
    }
}
