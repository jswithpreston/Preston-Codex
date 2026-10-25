package com.preston.codex.data.database;

import androidx.room.RoomDatabase;

import com.preston.codex.domain.TransactionRunner;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Room implementation of {@link TransactionRunner}.
 */
public final class RoomTransactionRunner implements TransactionRunner {

    private final RoomDatabase database;

    public RoomTransactionRunner(RoomDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
    }

    @Override
    public <T> T runInTransaction(Supplier<T> action) {
        return database.runInTransaction((java.util.concurrent.Callable<T>) action::get);
    }

    @Override
    public void runInTransaction(Runnable action) {
        database.runInTransaction(action);
    }
}
