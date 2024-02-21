/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.ewb.datastores.energy;

import com.zepben.energy.datastore.ErrorHandler;
import com.zepben.energy.datastore.blobstore.Deserialisers;
import com.zepben.energy.datastore.blobstore.Serialisers;
import com.zepben.energy.model.Channel;
import com.zepben.energy.model.EnergyProfile;
import com.zepben.energy.model.IdDateRange;
import com.zepben.energy.model.Readings;
import com.zepben.ewb.filepaths.EwbDataFilePaths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;

import static com.zepben.ewb.datastores.energy.SqliteByDateBlobStoreProvider.METADATA_DATE_ID;
import static com.zepben.ewb.datastores.energy.SqliteByDateBlobStoreProvider.METADATA_TIME_ZONE_ID;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SqliteEwbEnergyProfileStoreTest {

    private final LocalDate date = LocalDate.now(ZoneId.systemDefault());
    private final ZoneId timeZone = ZoneId.systemDefault();
    private EwbDataFilePaths paths;
    private SqliteEwbEnergyProfileStore store;

    @BeforeEach
    public void before(@TempDir Path tempDir) {
        paths = new EwbDataFilePaths(tempDir);
        store = new SqliteEwbEnergyProfileStore(paths, timeZone, EwbChannelFactory.DOUBLE_VALUES);
    }

    @AfterEach
    public void after() {
        store.close();
    }

    @Test
    public void writesReadsUpdates() {
        Readings kwIn = Readings.of(Channel.of(1., 2., 3.));
        Readings kwOut = Readings.of(Channel.of(4., 5., 6.));
        EnergyProfile profile = EnergyProfile.of("id", date, kwIn, kwOut, true);

        ErrorHandler onError = mock(ErrorHandler.class);
        store.writer().write(profile, onError);
        store.writer().commit(onError);
        verify(onError, never()).handle(any(), any(), any(), any());
        assertTrue(Files.exists(paths.energyReadings(LocalDate.now(ZoneId.systemDefault()))));

        EnergyProfile getProfile = store.reader().get("id", date, mock(ErrorHandler.class));
        verify(onError, never()).handle(any(), any(), any(), any());
        assertThat(getProfile, equalTo(profile));

        kwIn = Readings.of(Channel.of(7., 8., 9.));
        kwOut = Readings.of(Channel.of(10., 11., 12.));
        profile = EnergyProfile.of("id", date, kwIn, kwOut, false);
        store.writer().write(profile, onError);
        store.writer().commit(onError);
        verify(onError, never()).handle(any(), any(), any(), any());

        getProfile = store.reader().get("id", date, mock(ErrorHandler.class));
        verify(onError, never()).handle(any(), any(), any(), any());
        assertThat(getProfile, equalTo(profile));
    }

    @Test
    public void writesIndex() {
        Readings kwIn = Readings.of(Channel.of(1, 2, 3));
        Readings kwOut = Readings.of(Channel.of(4, 5, 6));
        EnergyProfile profile = EnergyProfile.of("id", date, kwIn, kwOut, true);

        ErrorHandler onError = mock(ErrorHandler.class);
        store.writer().write(profile, onError);
        store.writer().commit(onError);

        IdDateRange range = store.reader().getDateRange("id");
        assertNotNull(range);
        assertThat(range, is(new IdDateRange("id", date, LocalDate.now(ZoneId.systemDefault()))));

        profile = EnergyProfile.of("id", date.plusDays(1), kwIn, kwOut, true);
        onError = mock(ErrorHandler.class);
        store.writer().write(profile, onError);
        store.writer().commit(onError);

        range = store.reader().getDateRange("id");
        assertNotNull(range);
        assertThat(range, is(new IdDateRange("id", date, date.plusDays(1))));

        profile = EnergyProfile.of("id", date.minusDays(1), kwIn, kwOut, true);
        onError = mock(ErrorHandler.class);
        store.writer().write(profile, onError);
        store.writer().commit(onError);

        range = store.reader().getDateRange("id");
        assertNotNull(range);
        assertThat(range, is(new IdDateRange("id", date.minusDays(1), date.plusDays(1))));
    }

    @Test
    public void hasMetaData() throws Exception {
        // Write a profile to make sure the file exists
        EnergyProfile profile = EnergyProfile.of("id", date, null, null, true);
        ErrorHandler onError = mock(ErrorHandler.class);
        store.writer().write(profile, onError);

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:file:" + paths.energyReadings(date))) {
            try (Statement stmt = connection.createStatement()) {
                String sqlFormat = "select value from metadata where key = '%s'";
                try (ResultSet rs = stmt.executeQuery(String.format(sqlFormat, METADATA_DATE_ID))) {
                    assertTrue(rs.next());
                    assertThat(rs.getString(1), equalTo(date.toString()));
                }

                try (ResultSet rs = stmt.executeQuery(String.format(sqlFormat, METADATA_TIME_ZONE_ID))) {
                    assertTrue(rs.next());
                    assertThat(rs.getString(1), equalTo(timeZone.getId()));
                }
            }
        }
    }

    @Test
    public void serialisers() {
        Serialisers serialisers = store.serialisers();
        assertThat(serialisers.kwInSx(), instanceOf(ReadingsSerialiser.class));
        assertThat(serialisers.kwOutSx(), instanceOf(ReadingsSerialiser.class));
        assertThat(serialisers.cacheableSx(), instanceOf(CacheableSerialiser.class));
    }

    @Test
    public void deserialisers() {
        Deserialisers deserialisers = store.deserialisers();
        assertThat(deserialisers.kwInDsx(), instanceOf(ReadingsDeserialiser.class));
        assertThat(deserialisers.kwOutDsx(), instanceOf(ReadingsDeserialiser.class));
        assertThat(deserialisers.cacheableDsx(), instanceOf(CacheableDeserialiser.class));
    }

}
