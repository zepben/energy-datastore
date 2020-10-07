/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.datastore.blobstore.indexing;

import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.blobstore.BlobReader;
import com.zepben.blobstore.BlobStore;
import com.zepben.blobstore.BlobStoreException;
import com.zepben.blobstore.BlobWriter;
import com.zepben.energy.model.IdDateRange;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.Collection;
import java.util.function.Consumer;

@EverythingIsNonnullByDefault
public class BlobDateRangeIndex implements DateRangeIndex, AutoCloseable {

    public static final String STORE_TAG = "dateRange";

    private final BlobStore blobStore;
    private final BlobReader reader;
    private final BlobWriter writer;
    private final IdDateRangeCodec codec = new IdDateRangeCodec();

    public BlobDateRangeIndex(BlobStore blobStore) {
        this.blobStore = blobStore;
        reader = blobStore.reader();
        writer = blobStore.writer();
    }

    @Override
    public void close() throws BlobStoreException {
        blobStore.close();
    }

    @Nullable
    @Override
    public IdDateRange get(String id) {
        try {
            byte[] bytes = reader.get(id, STORE_TAG);
            return codec.deserialise(id, bytes);
        } catch (BlobStoreException e) {
            return null;
        }
    }

    @Override
    public void forEach(Collection<String> ids, Consumer<IdDateRange> handler) {
        try {
            reader.forEach(ids, STORE_TAG, (id, tag, blob) -> {
                IdDateRange dateRange = codec.deserialise(id, blob);
                if (dateRange != null)
                    handler.accept(dateRange);
            });
        } catch (BlobStoreException e) {
            // TODO: What to do with this exception?
        }
    }

    @Override
    public void forAll(Consumer<IdDateRange> handler) {
        try {
            reader.forAll(STORE_TAG, (id, tag, blob) -> {
                IdDateRange dateRange = codec.deserialise(id, blob);
                if (dateRange != null)
                    handler.accept(dateRange);
            });
        } catch (BlobStoreException e) {
            // TODO: What to do with this exception?
        }
    }

    @Override
    public boolean save(String id, LocalDate from, LocalDate to) {
        byte[] bytes = codec.serialise(from, to);
        try {
            return writer.update(id, STORE_TAG, bytes) || writer.write(id, STORE_TAG, bytes);
        } catch (BlobStoreException e) {
            // TODO: What to do with this exception?
            return false;
        }
    }

    @Override
    public boolean commit() {
        try {
            writer.commit();
            return true;
        } catch (BlobStoreException e) {
            // TODO: What to do with this exception?
            return false;
        }
    }

    @Override
    public boolean rollback() {
        try {
            writer.rollback();
            return true;
        } catch (BlobStoreException e) {
            // TODO: What to do with this exception?
            return false;
        }
    }

}
