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
import com.zepben.blobstore.sqlite.SqliteBlobStore;
import com.zepben.energy.datastore.blobstore.EnergyProfileAttribute;
import com.zepben.evolve.database.paths.EwbDataFilePaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;

@EverythingIsNonnullByDefault
class SqliteByDateBlobStoreProvider implements ByDateBlobStoreProvider {

    static final String METADATA_DATE_ID = "date";
    static final String METADATA_TIME_ZONE_ID = "timezone";

    private final EwbDataFilePaths ewbPaths;

    SqliteByDateBlobStoreProvider(EwbDataFilePaths ewbPaths) {
        this.ewbPaths = ewbPaths;
    }

    @Nullable
    @Override
    public SqliteBlobStore get(LocalDate date, ZoneId timeZone, boolean createIfNotExists) throws BlobStoreException {
        SqliteBlobStore blobStore = null;
        boolean needsClosing = true;
        try {
            Path path = ewbPaths.energyReadings(date);
            if (!Files.exists(path) && !createIfNotExists)
                return null;

            ewbPaths.createDirectories(date);

            blobStore = new SqliteBlobStore(path, EnergyProfileAttribute.storeTagSet());

            ZonedDateTime zdt = getDateMetaData(blobStore);
            if (zdt == null) {
                writeDateMetadata(blobStore, date, timeZone);
            } else if (!zdt.toLocalDate().equals(date)) {
                throw new BlobStoreException(
                    String.format("metadata %s was '%s', expected '%s'", METADATA_DATE_ID, zdt.toLocalDate(), date),
                    null);
            } else if (!zdt.getZone().equals(timeZone)) {
                throw new BlobStoreException(
                    String.format("metadata %s was '%s', expected '%s'", METADATA_TIME_ZONE_ID, zdt.getZone(), timeZone),
                    null);
            }

            needsClosing = false;
            return blobStore;
        } catch (IOException e) {
            throw new BlobStoreException(String.format("failed to create path for date %s", date), e);
        } catch (DateTimeException e) {
            throw new BlobStoreException("invalid metadata", e);
        } finally {
            if (needsClosing && blobStore != null)
                blobStore.close();
        }
    }

    @Nullable
    private ZonedDateTime getDateMetaData(SqliteBlobStore blobStore) throws BlobStoreException {
        LocalDate date = getMetaDataDate(blobStore);
        ZoneId timeZone = getMetaDataTimeZone(blobStore);

        if (date == null && timeZone == null)
            return null;
        else if (date == null) {
            throw new BlobStoreException("invalid metadata: date defined with no time zone", null);
        } else if (timeZone == null) {
            throw new BlobStoreException("invalid metadata: time zone defined with no date", null);
        }

        return ZonedDateTime.of(date, LocalTime.MIDNIGHT, timeZone);
    }

    @Nullable
    private LocalDate getMetaDataDate(SqliteBlobStore blobStore) throws BlobStoreException {
        String date = blobStore.getReader().getMetadata(METADATA_DATE_ID);
        if (date == null)
            return null;

        return LocalDate.parse(date);
    }

    @Nullable
    private ZoneId getMetaDataTimeZone(SqliteBlobStore blobStore) throws BlobStoreException {
        String timeZone = blobStore.getReader().getMetadata(METADATA_TIME_ZONE_ID);
        if (timeZone == null)
            return null;

        return ZoneId.of(timeZone);
    }

    private void writeDateMetadata(SqliteBlobStore blobStore, LocalDate date, ZoneId zoneId) throws BlobStoreException {
        blobStore.getWriter().writeMetadata(METADATA_DATE_ID, date.toString());
        blobStore.getWriter().writeMetadata(METADATA_TIME_ZONE_ID, zoneId.getId());
        blobStore.getWriter().commit();
    }

}
