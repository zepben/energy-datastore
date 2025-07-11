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
import com.zepben.blobstore.BlobStoreException;
import com.zepben.blobstore.itemwrappers.ByDateBlobReaderProvider;
import com.zepben.energy.datastore.blobstore.indexing.BlobDateRangeIndex;
import com.zepben.ewb.database.paths.DatabaseType;
import com.zepben.ewb.database.paths.EwbDataFilePaths;
import kotlin.Unit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@SuppressWarnings("WeakerAccess")
@EverythingIsNonnullByDefault
public class EwbEnergyProfileStoreReindexer {

    private final EwbDataFilePaths ewbPaths;
    private final Progress.Factory progressFactory;
    private final ZoneId timeZone;
    private final ByDateBlobReaderProvider byDateStoreProvider;
    private final Supplier<BlobDateRangeIndex> indexSupplier;

    public static EwbEnergyProfileStoreReindexer create(EwbDataFilePaths ewbPaths,
                                                        ZoneId timeZone,
                                                        Progress.Factory progressFactory) {
        return new EwbEnergyProfileStoreReindexer(
            ewbPaths,
            timeZone,
            SqliteEwbEnergyProfileStore.createByDateBlobStoreCache(ewbPaths),
            () -> SqliteEwbEnergyProfileStore.createEnergyProfileIndex(ewbPaths),
            progressFactory);
    }

    EwbEnergyProfileStoreReindexer(EwbDataFilePaths ewbPaths,
                                   ZoneId timeZone,
                                   ByDateBlobReaderProvider byDateStoreProvider,
                                   Supplier<BlobDateRangeIndex> indexSupplier,
                                   Progress.Factory progressFactory) {
        this.ewbPaths = ewbPaths;
        this.timeZone = timeZone;
        this.byDateStoreProvider = byDateStoreProvider;
        this.indexSupplier = indexSupplier;
        this.progressFactory = progressFactory;
    }

    @SuppressWarnings("WeakerAccess")
    public void reindex() throws BlobStoreException {
        List<LocalDate> dates = getAvailableDates();
        Map<String, Range> index = buildIndex(dates);
        writeIndex(index);
    }

    private Map<String, Range> buildIndex(List<LocalDate> dates) throws BlobStoreException {
        Progress progress = progressFactory.create("Building index", dates.size());
        Map<String, Range> index = new HashMap<>();

        for (LocalDate date : dates) {
            try (BlobReader readingsStore = byDateStoreProvider.getReader(date, timeZone)) {
                if (readingsStore != null) {
                    readingsStore.ids(id -> {
                        Range range = index.computeIfAbsent(id, i -> new Range(i, date, date));
                        if (date.isBefore(range.from))
                            range.from = date;

                        if (date.isAfter(range.to))
                            range.to = date;

                        return Unit.INSTANCE;
                    });
                }
            }

            progress.step();
        }

        return index;
    }

    private void writeIndex(Map<String, Range> index) throws BlobStoreException {
        Progress progress = progressFactory.create("Saving index", index.size());
        Path backupPath = Paths.get(ewbPaths.resolve(DatabaseType.ENERGY_READINGS_INDEX) + ".bak");
        backupIndex(backupPath);

        boolean status = true;
        try (BlobDateRangeIndex indexStore = indexSupplier.get()) {
            for (Range range : index.values()) {
                status &= indexStore.save(range.id, range.from, range.to);
                progress.step();
            }

            status &= indexStore.commit();
        }

        if (!status) {
            restoreIndex(backupPath);
            throw new BlobStoreException("Failed to write to index database.", null);
        }

        deleteBackup(backupPath);
    }

    private List<LocalDate> getAvailableDates() throws BlobStoreException {
        try {
            return ewbPaths.getAvailableDatesFor(DatabaseType.ENERGY_READING);
        } catch (Exception ex) {
            throw new BlobStoreException("Failed to read date listing from " + ewbPaths.getClass().getSimpleName(), ex);
        }
    }

    private void backupIndex(Path backupPath) throws BlobStoreException {
        try {
            if (Files.exists(ewbPaths.resolve(DatabaseType.ENERGY_READINGS_INDEX)))
                Files.move(ewbPaths.resolve(DatabaseType.ENERGY_READINGS_INDEX), backupPath);
        } catch (IOException e) {
            throw new BlobStoreException("Failed to backup index file", e);
        }
    }

    private void deleteBackup(Path backupPath) throws BlobStoreException {
        try {
            Files.deleteIfExists(backupPath);
        } catch (IOException e) {
            throw new BlobStoreException("Failed to delete backup file", e);
        }
    }

    private void restoreIndex(Path backupPath) throws BlobStoreException {
        try {
            Files.deleteIfExists(ewbPaths.resolve(DatabaseType.ENERGY_READINGS_INDEX));
            if (Files.exists(backupPath))
                Files.move(backupPath, ewbPaths.resolve(DatabaseType.ENERGY_READINGS_INDEX));
        } catch (IOException ex) {
            throw new BlobStoreException(
                "Failed to restore index. You need to manually restore from backup file: " + backupPath,
                ex);
        }
    }

    @EverythingIsNonnullByDefault
    private static class Range {

        private final String id;
        private LocalDate from;
        private LocalDate to;

        Range(String id, LocalDate from, LocalDate to) {
            this.id = id;
            this.from = from;
            this.to = to;
        }

    }

}
