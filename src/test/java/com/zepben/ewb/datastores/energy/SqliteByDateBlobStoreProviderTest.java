/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.ewb.datastores.energy;

import com.zepben.blobstore.BlobStoreException;
import com.zepben.blobstore.sqlite.SqliteBlobStore;
import com.zepben.ewb.database.paths.EwbDataFilePaths;
import com.zepben.ewb.database.paths.LocalEwbDataFilePaths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;

import static com.zepben.ewb.datastores.energy.SqliteByDateBlobStoreProvider.METADATA_DATE_ID;
import static com.zepben.ewb.datastores.energy.SqliteByDateBlobStoreProvider.METADATA_TIME_ZONE_ID;
import static com.zepben.testutils.exception.ExpectException.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SqliteByDateBlobStoreProviderTest {

    private File folder;
    private SqliteByDateBlobStoreProvider provider;
    private final LocalDate date = LocalDate.now(ZoneId.systemDefault());
    private final ZoneId timeZone = ZoneId.of("Australia/Canberra");
    private SqliteBlobStore blobStore = null;

    @BeforeEach
    public void before(@TempDir Path tempDir) {
        folder = tempDir.toFile();
        EwbDataFilePaths ewbFilePaths = new LocalEwbDataFilePaths(tempDir.toString());
        provider = new SqliteByDateBlobStoreProvider(ewbFilePaths);
    }

    @AfterEach
    public void after() throws Exception {
        if (blobStore != null)
            blobStore.close();
    }

    @Test
    public void doesNotCreateStoreWhenNotToldTo() throws Exception {
        blobStore = provider.get(date, timeZone, false);
        assertThat(blobStore, is(nullValue()));
    }

    @Test
    public void createsStoreWhenToldTo() throws Exception {
        blobStore = provider.get(date, timeZone, true);
        assertThat(blobStore, not(nullValue()));

        Path expectedPath = Paths.get(folder.toString(), date.toString());
        assertThat(Files.exists(expectedPath), is(true));
    }

    @Test
    public void populatesDateMetadata() throws Exception {
        blobStore = provider.get(date, timeZone, true);
        assertNotNull(blobStore);

        String dateStr = blobStore.getReader().getMetadata(METADATA_DATE_ID);
        assertNotNull(dateStr);
        LocalDate getDate = LocalDate.parse(dateStr);
        assertThat(getDate, equalTo(date));
    }

    @Test
    public void populatesTimeZoneMetadata() throws Exception {
        blobStore = provider.get(date, timeZone, true);
        assertNotNull(blobStore);

        String tzStr = blobStore.getReader().getMetadata(METADATA_TIME_ZONE_ID);
        assertNotNull(tzStr);
        ZoneId getTz = ZoneId.of(tzStr);
        assertThat(getTz, equalTo(timeZone));
    }

    @Test
    public void validatesExistingFileMetadata() throws Exception {
        blobStore = provider.get(date, timeZone, true);
        assertNotNull(blobStore);
        blobStore.close();

        blobStore = provider.get(date, timeZone, true);
        assertNotNull(blobStore);
    }

    @Test
    public void throwsWhenDateDoesNotMatch() throws Exception {
        blobStore = provider.get(date, timeZone, true);
        assertNotNull(blobStore);

        LocalDate newDate = date.plusDays(1);
        blobStore.getWriter().updateMetadata(METADATA_DATE_ID, newDate.toString());
        blobStore.getWriter().commit();
        blobStore.close();

        String msg = String.format("metadata %s was '%s', expected '%s'", METADATA_DATE_ID, newDate, date);
        expect(() -> blobStore = provider.get(date, timeZone, true))
            .toThrow(BlobStoreException.class)
            .withMessage(msg);
    }

    @Test
    public void throwsWhenDateButNoTimeZone() throws Exception {
        blobStore = provider.get(date, timeZone, true);
        assertNotNull(blobStore);

        blobStore.getWriter().deleteMetadata(METADATA_DATE_ID);
        blobStore.getWriter().commit();
        blobStore.close();

        expect(() -> blobStore = provider.get(date, timeZone, true))
            .toThrow(BlobStoreException.class)
            .withMessage("invalid metadata: date defined with no time zone");
    }

    @Test
    public void throwsWhenTimeZoneDoesNotMatch() throws Exception {
        blobStore = provider.get(date, timeZone, true);
        assertNotNull(blobStore);

        ZoneId newTimeZone = ZoneId.of("Z");
        blobStore.getWriter().updateMetadata(METADATA_TIME_ZONE_ID, newTimeZone.toString());
        blobStore.getWriter().commit();
        blobStore.close();

        String msg = String.format("metadata %s was '%s', expected '%s'", METADATA_TIME_ZONE_ID, newTimeZone, timeZone);
        expect(() -> blobStore = provider.get(date, timeZone, true))
            .toThrow(BlobStoreException.class)
            .withMessage(msg);
    }

    @Test
    public void throwsWhenTimeZoneButNoDate() throws Exception {
        blobStore = provider.get(date, timeZone, true);
        assertNotNull(blobStore);

        blobStore.getWriter().deleteMetadata(METADATA_TIME_ZONE_ID);
        blobStore.getWriter().commit();
        blobStore.close();

        expect(() -> blobStore = provider.get(date, timeZone, true))
            .toThrow(BlobStoreException.class)
            .withMessage("invalid metadata: time zone defined with no date");
    }

}
