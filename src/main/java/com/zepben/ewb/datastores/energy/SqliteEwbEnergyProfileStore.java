/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.ewb.datastores.energy;

import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.blobstore.BlobStoreException;
import com.zepben.blobstore.itemwrappers.ByDateItemReader;
import com.zepben.blobstore.itemwrappers.ByDateItemWriter;
import com.zepben.blobstore.sqlite.SqliteBlobStore;
import com.zepben.energy.datastore.EnergyProfileReader;
import com.zepben.energy.datastore.EnergyProfileStore;
import com.zepben.energy.datastore.EnergyProfileWriter;
import com.zepben.energy.datastore.blobstore.ByDateBlobEnergyProfileReader;
import com.zepben.energy.datastore.blobstore.ByDateBlobEnergyProfileWriter;
import com.zepben.energy.datastore.blobstore.Deserialisers;
import com.zepben.energy.datastore.blobstore.Serialisers;
import com.zepben.energy.datastore.blobstore.indexing.BlobDateRangeIndex;
import com.zepben.energy.datastore.blobstore.indexing.CachedDateRangeIndex;
import com.zepben.energy.datastore.blobstore.indexing.DateRangeIndex;
import com.zepben.energy.model.EnergyProfile;
import com.zepben.evolve.database.paths.DatabaseType;
import com.zepben.evolve.database.paths.EwbDataFilePaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.util.Collections;

/**
 * Creates a energy profile reader and writer for use with EWB and its tools.
 */
@EverythingIsNonnullByDefault
public class SqliteEwbEnergyProfileStore implements EnergyProfileStore {

    private final Logger log = LoggerFactory.getLogger(SqliteEwbEnergyProfileStore.class);

    private final BlobDateRangeIndex dateRangeIndex;
    private final ByDateBlobStoreCache storeProvider;
    private final Serialisers serialisers;
    private final Deserialisers deserialisers;
    private final EnergyProfileReader reader;
    private final EnergyProfileWriter writer;

    @SuppressWarnings("WeakerAccess")
    public SqliteEwbEnergyProfileStore(EwbDataFilePaths ewbPaths,
                                       ZoneId timeZone,
                                       EwbChannelFactory channelFactory) {
        storeProvider = createByDateBlobStoreCache(ewbPaths);
        dateRangeIndex = createEnergyProfileIndex(ewbPaths);
        DateRangeIndex cachedIndex = new CachedDateRangeIndex(dateRangeIndex);

        ByDateItemReader<EnergyProfile> itemReader = new ByDateItemReader<>(timeZone, storeProvider);

        serialisers = new Serialisers(
            new ReadingsSerialiser(),
            new ReadingsSerialiser(),
            new CacheableSerialiser(),
            new EnergyProfileStatSerialiser());

        deserialisers = new Deserialisers(
            new ReadingsDeserialiser(channelFactory),
            new ReadingsDeserialiser(channelFactory),
            new CacheableDeserialiser(),
            new EnergyProfileStatDeserialiser());

        reader = new ByDateBlobEnergyProfileReader(
            cachedIndex,
            itemReader,
            EnergyProfile::of,
            deserialisers);

        ByDateItemWriter itemWriter = new ByDateItemWriter(timeZone, storeProvider);
        writer = new ByDateBlobEnergyProfileWriter(cachedIndex, itemWriter, serialisers);
    }

    static BlobDateRangeIndex createEnergyProfileIndex(EwbDataFilePaths ewbPaths) {
        SqliteBlobStore dateRangeIndexStore = new SqliteBlobStore(ewbPaths.resolve(DatabaseType.ENERGY_READINGS_INDEX), Collections.singleton(BlobDateRangeIndex.STORE_TAG));
        return new BlobDateRangeIndex(dateRangeIndexStore);
    }

    static ByDateBlobStoreCache createByDateBlobStoreCache(EwbDataFilePaths ewbPaths) {
        SqliteByDateBlobStoreProvider blobStoreProvider = new SqliteByDateBlobStoreProvider(ewbPaths);
        return new ByDateBlobStoreCache(blobStoreProvider);
    }

    @Override
    public EnergyProfileReader reader() {
        return reader;
    }

    @Override
    public EnergyProfileWriter writer() {
        return writer;
    }

    @Override
    public void close() {
        storeProvider.close((store, date, error) -> log.error("Failed to close sqlite energy profile store for " + date, error));

        try {
            dateRangeIndex.close();
        } catch (BlobStoreException e) {
            log.error("Failed to close sqlite energy profile index db", e);
        }
    }

    Serialisers serialisers() {
        return serialisers;
    }

    Deserialisers deserialisers() {
        return deserialisers;
    }

}
