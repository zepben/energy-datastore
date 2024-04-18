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
import com.zepben.energy.datastore.ErrorHandler;
import com.zepben.energy.datastore.blobstore.indexing.BlobDateRangeIndex;
import com.zepben.energy.model.EnergyProfile;
import com.zepben.energy.model.IdDateRange;
import com.zepben.evolve.database.paths.EwbDataFilePaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

import static com.zepben.testutils.exception.ExpectException.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class EwbEnergyProfileStoreReindexerTest {

    private final Progress progress = mock(Progress.class);
    private final Progress.Factory progressFactory = mock(Progress.Factory.class);

    private final LocalDate date = LocalDate.now(ZoneId.systemDefault());
    private final ZoneId timeZone = ZoneId.systemDefault();
    private EwbDataFilePaths paths;
    private EwbEnergyProfileStoreReindexer reindexer;

    @BeforeEach
    public void before(@TempDir Path tempDir) {
        doReturn(progress).when(progressFactory).create(any(), anyInt());

        paths = new EwbDataFilePaths(tempDir.toString());

        createReadingsFiles();
        reindexer = EwbEnergyProfileStoreReindexer.create(paths, timeZone, progressFactory);
    }

    private void createReadingsFiles() {
        ErrorHandler onError = (id, dt, msg, t) -> {
            throw new RuntimeException(t);
        };

        try (SqliteEwbEnergyProfileStore store = new SqliteEwbEnergyProfileStore(paths, timeZone, EwbChannelFactory.DOUBLE_VALUES)) {
            LocalDate dateMinus1 = date.minusDays(1);
            LocalDate datePlus1 = date.plusDays(1);
            store.writer().write(EnergyProfile.of("allDays", dateMinus1, null, null), onError);
            store.writer().write(EnergyProfile.of("allDays", date, null, null), onError);
            store.writer().write(EnergyProfile.of("allDays", datePlus1, null, null), onError);
            store.writer().write(EnergyProfile.of("singleDay", date, null, null), onError);
            store.writer().commit(onError);
        }
    }

    @Test
    public void reindexes() throws Exception {
        reindexer.reindex();

        assertThat(Files.exists(paths.energyReadingsIndex()), is(true));
        try (SqliteBlobStore blobIndex = new SqliteBlobStore(paths.energyReadingsIndex(), Collections.singleton(BlobDateRangeIndex.STORE_TAG))) {
            BlobDateRangeIndex index = new BlobDateRangeIndex(blobIndex);

            IdDateRange range = index.get("allDays");
            IdDateRange expectedRange = new IdDateRange("allDays", date.minusDays(1), date.plusDays(1));
            assertThat(range, equalTo(expectedRange));

            range = index.get("singleDay");
            expectedRange = new IdDateRange("singleDay", date, date);
            assertThat(range, equalTo(expectedRange));
        }
    }

    @Test
    public void reindexesMissingIndexFileDoesNotCauseFailure() throws Exception {
        Files.delete(paths.energyReadingsIndex());
        reindexer.reindex();
    }

    @Test
    public void restoresIfCanNotWrite() throws Exception {
        // Blow away the index that is there and create a rubbish file that we can test gets restored
        Files.delete(paths.energyReadingsIndex());
        Files.write(paths.energyReadingsIndex(), Arrays.asList("something", "to test"));

        BlobDateRangeIndex index = mock(BlobDateRangeIndex.class);
        doReturn(false).when(index).commit();
        reindexer = new EwbEnergyProfileStoreReindexer(
            paths,
            timeZone,
            SqliteEwbEnergyProfileStore.createByDateBlobStoreCache(paths),
            () -> index,
            progressFactory);

        expect(() -> reindexer.reindex()).toThrow(BlobStoreException.class).withMessage("Failed to write to index database.");
        assertThat(Files.readAllLines(paths.energyReadingsIndex()), equalTo(Arrays.asList("something", "to test")));
    }

    @Test
    public void usesProgress() throws Exception {
        Progress build = mock(Progress.class);
        Progress save = mock(Progress.class);
        doReturn(build, save).when(progressFactory).create(any(), anyInt());

        reindexer.reindex();

        verify(progressFactory).create("Building index", 3);
        verify(progressFactory).create("Saving index", 2);

        verify(build, times(3)).step();
        verify(save, times(2)).step();
    }

}
