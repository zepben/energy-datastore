/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.ewb.datastores.energy;

import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.blobstore.BlobReader;
import com.zepben.blobstore.BlobStore;
import com.zepben.blobstore.BlobStoreException;
import com.zepben.blobstore.BlobWriter;
import com.zepben.blobstore.itemwrappers.ByDateBlobReaderProvider;
import com.zepben.blobstore.itemwrappers.ByDateBlobWriterProvider;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides the blob stores to be used by the EWB energy profile reader/writer instances.
 */
@EverythingIsNonnullByDefault
class ByDateBlobStoreCache implements ByDateBlobWriterProvider, ByDateBlobReaderProvider {

    private final ByDateBlobStoreProvider factory;
    private Map<LocalDate, BlobStore> cache = new ConcurrentHashMap<>();

    @EverythingIsNonnullByDefault
    interface ErrorHandler {

        void handle(BlobStore store, LocalDate date, Throwable error);

    }

    ByDateBlobStoreCache(ByDateBlobStoreProvider factory) {
        this.factory = factory;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public BlobWriter getWriter(LocalDate date, ZoneId timeZone) throws BlobStoreException {
        BlobStore store = getStore(date, timeZone, true);
        if (store == null)
            throw new IllegalStateException("blobstore should never be null for writing");
        return store.getWriter();
    }

    @Override
    @Nullable
    public BlobReader getReader(LocalDate date, ZoneId timeZone) throws BlobStoreException {
        BlobStore store = getStore(date, timeZone, false);
        return store == null ? null : store.getReader();
    }

    @Nullable
    private BlobStore getStore(LocalDate date, ZoneId timeZone, boolean createIfNotExists) throws BlobStoreException {
        BlobStore store = cache.get(date);
        if (store == null) {
            store = factory.get(date, timeZone, createIfNotExists);
            if (store != null)
                cache.put(date, store);
        }
        return store;
    }

    void close(ErrorHandler handler) {
        Map<LocalDate, BlobStore> failed = new ConcurrentHashMap<>();
        cache.forEach((date, store) -> {
            try {
                store.close();
            } catch (BlobStoreException e) {
                failed.put(date, store);
                handler.handle(store, date, e);
            }
        });
        cache = failed;
    }

}
