/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.datastore.blobstore;

import com.zepben.blobstore.BlobStoreException;
import com.zepben.blobstore.BlobWriter;
import com.zepben.blobstore.itemwrappers.ByDateItemWriter;
import com.zepben.energy.datastore.EnergyProfileWriter;
import com.zepben.energy.datastore.ErrorHandler;
import com.zepben.energy.datastore.blobstore.indexing.DateRangeIndex;
import com.zepben.energy.datastore.blobstore.indexing.MockDateRangeIndex;
import com.zepben.energy.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;

import static com.zepben.energy.datastore.blobstore.EnergyProfileAttribute.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ByDateBlobEnergyProfileWriterTest {

    private final String id = "id";
    private final LocalDate date = LocalDate.now(ZoneId.systemDefault());
    private final ZoneId timeZone = ZoneId.systemDefault();

    private final DateRangeIndex dateRangeIndex = spy(new MockDateRangeIndex(Collections.emptyList()));

    @Mock private BlobWriter blobWriter;
    @Mock private Serialiser<Readings> kwInSx;
    @Mock private Serialiser<Readings> kwOutSx;
    @Mock private Serialiser<Boolean> cacheableSx;
    @Mock private Serialiser<EnergyProfileStat> statSx;
    private final ByDateItemWriter byDateItemWriter = spy(new ByDateItemWriter(timeZone, (date, tz) -> blobWriter));
    private EnergyProfileWriter profileWriter;
    private final ErrorHandler onError = mock(ErrorHandler.class);

    private final byte[] kwInBytes = new byte[]{2};
    private final byte[] kwOutBytes = new byte[]{3};
    private final byte[] cacheableBytes = new byte[]{1};
    private final byte[] statBytes = new byte[]{4};

    @BeforeEach
    public void before() throws Exception {
        MockitoAnnotations.openMocks(this).close();

        setupSerialisers();
        Serialisers serialisers = new Serialisers(kwInSx, kwOutSx, cacheableSx, statSx);

        profileWriter = new ByDateBlobEnergyProfileWriter(dateRangeIndex, byDateItemWriter, serialisers);
    }

    private void setupSerialisers() {
        when(kwInSx.sx(any())).thenReturn(kwInBytes);
        when(kwInSx.sxOffset()).thenReturn(0);
        when(kwInSx.sxLength()).thenReturn(kwInBytes.length);

        when(kwOutSx.sx(any())).thenReturn(kwOutBytes);
        when(kwOutSx.sxOffset()).thenReturn(0);
        when(kwOutSx.sxLength()).thenReturn(kwOutBytes.length);

        when(cacheableSx.sx(any())).thenReturn(cacheableBytes);
        when(cacheableSx.sxOffset()).thenReturn(0);
        when(cacheableSx.sxLength()).thenReturn(cacheableBytes.length);

        when(statSx.sx(any())).thenReturn(statBytes);
        when(statSx.sxOffset()).thenReturn(0);
        when(statSx.sxLength()).thenReturn(statBytes.length);
    }

    @SuppressWarnings("SameParameterValue")
    private EnergyProfile newProfile(String id, LocalDate date, boolean cacheable) {
        EnergyProfile mock = mock(EnergyProfile.class);
        when(mock.id()).thenReturn(id);
        when(mock.date()).thenReturn(date);
        when(mock.kwIn()).thenReturn(Readings.of(Channel.of(1)));
        when(mock.kwOut()).thenReturn(Readings.of(Channel.of(2)));
        when(mock.cacheable()).thenReturn(cacheable);
        return mock;
    }

    private void setupBlobWriter(boolean write, boolean update, boolean delete) throws BlobStoreException {
        when(blobWriter.write(anyString(), anyString(), any(byte[].class), anyInt(), anyInt())).thenReturn(write);
        when(blobWriter.update(anyString(), anyString(), any(byte[].class), anyInt(), anyInt())).thenReturn(update);
        when(blobWriter.delete(anyString(), anyString())).thenReturn(delete);
    }

    @Test
    public void writesCacheableProfile() throws BlobStoreException {
        EnergyProfile profile = newProfile(id, date, true);

        setupBlobWriter(true, false, false);

        assertTrue(profileWriter.write(profile, onError));
        verify(blobWriter, times(3)).write(anyString(), anyString(), any(byte[].class), anyInt(), anyInt());
        verify(blobWriter, atMost(1)).write(profile.id(), KW_IN.storeString(), kwInBytes, 0, kwInBytes.length);
        verify(blobWriter, atMost(1)).write(profile.id(), KW_OUT.storeString(), kwInBytes, 0, kwInBytes.length);
        verify(blobWriter, atMost(1)).write(profile.id(), CACHEABLE.storeString(), new byte[]{1}, 0, 1);
        verify(profile).kwIn();
        verify(profile).kwOut();
        verify(profile).cacheable();
        verify(dateRangeIndex).extendRange(id, date);

        verify(onError, never()).handle(any(), any(), any(), any());
    }

    @Test
    public void writesNotCacheableProfile() throws BlobStoreException {
        EnergyProfile profile = newProfile(id, date, false);

        setupBlobWriter(true, false, true);

        assertTrue(profileWriter.write(profile, onError));
        verify(blobWriter, never()).write(profile.id(), CACHEABLE.storeString(), new byte[]{1}, 0, 1);
        verify(profile).cacheable();

        verify(onError, never()).handle(any(), any(), any(), any());
    }

    @Test
    public void updatesCacheableProfile() throws BlobStoreException {
        EnergyProfile profile = newProfile(id, date, true);

        setupBlobWriter(true, true, false);
        when(blobWriter.update(anyString(), anyString(), any(byte[].class), anyInt(), anyInt())).thenReturn(true, true, false);

        assertTrue(profileWriter.write(profile, onError));
        verify(blobWriter, times(1)).write(anyString(), anyString(), any(byte[].class), anyInt(), anyInt());
        verify(blobWriter, times(3)).update(anyString(), anyString(), any(byte[].class), anyInt(), anyInt());
        verify(blobWriter, atMost(1)).update(profile.id(), KW_IN.storeString(), kwInBytes, 0, kwInBytes.length);
        verify(blobWriter, atMost(1)).update(profile.id(), KW_OUT.storeString(), kwInBytes, 0, kwInBytes.length);
        verify(blobWriter, atMost(1)).write(profile.id(), CACHEABLE.storeString(), new byte[]{1}, 0, 1);
        verify(profile, times(1)).kwIn();
        verify(profile, times(1)).kwOut();
        verify(profile, times(1)).cacheable();
        verify(dateRangeIndex).extendRange(id, date);

        verify(onError, never()).handle(any(), any(), any(), any());
    }

    @Test
    public void updatesNotCacheableProfile() throws BlobStoreException {
        EnergyProfile profile = newProfile(id, date, true);

        setupBlobWriter(false, true, true);

        assertTrue(profileWriter.write(profile, onError));
        verify(blobWriter, atMost(1)).delete(profile.id(), CACHEABLE.storeString());
        verify(profile, times(1)).cacheable();

        verify(onError, never()).handle(any(), any(), any(), any());
    }

    @Test
    public void writesStats() throws Exception {
        EnergyProfile profile = newProfile(id, date, false);
        setupBlobWriter(true, false, true);

        assertTrue(profileWriter.write(profile, true, onError));
        verify(blobWriter, atMost(1)).update(profile.id(), MAXIMUMS.storeString(), statBytes, 0, statBytes.length);
        verify(blobWriter, atMost(1)).write(profile.id(), MAXIMUMS.storeString(), statBytes, 0, statBytes.length);
        verify(onError, never()).handle(any(), any(), any(), any());
    }

    @Test
    public void writesKwIn() throws BlobStoreException {
        Readings readings = Readings.of(Channel.of(1.));

        setupBlobWriter(true, false, false);

        assertTrue(profileWriter.writeKwIn("id", LocalDate.now(ZoneId.systemDefault()), readings, onError));
        verify(blobWriter).write("id", KW_IN.storeString(), kwInBytes, 0, kwInBytes.length);
        verify(onError, never()).handle(any(), any(), any(), any());
        verify(dateRangeIndex).extendRange(id, date);
    }

    @Test
    public void updatesKwIn() throws BlobStoreException {
        Readings readings = Readings.of(Channel.of(1.));

        setupBlobWriter(false, true, false);

        assertTrue(profileWriter.writeKwIn("id", LocalDate.now(ZoneId.systemDefault()), readings, onError));
        verify(blobWriter).update("id", KW_IN.storeString(), kwInBytes, 0, kwInBytes.length);
        verify(onError, never()).handle(any(), any(), any(), any());
        verify(dateRangeIndex).extendRange(id, date);
    }

    @Test
    public void writeKwInDeletesStats() throws Exception {
        Readings readings = Readings.of(Channel.of(1.));

        setupBlobWriter(true, false, true);

        assertTrue(profileWriter.writeKwIn("id", LocalDate.now(ZoneId.systemDefault()), readings, onError));
        verify(blobWriter).delete("id", MAXIMUMS.storeString());
    }

    @Test
    public void kwInError() throws BlobStoreException {
        Readings readings = Readings.of(Channel.of(1.));

        setupBlobWriter(false, false, false);

        assertFalse(profileWriter.writeKwIn("id", LocalDate.now(ZoneId.systemDefault()), readings, onError));
        verify(blobWriter).write("id", KW_IN.storeString(), kwInBytes, 0, kwInBytes.length);
        verify(blobWriter).update("id", KW_IN.storeString(), kwInBytes, 0, kwInBytes.length);
        verify(onError).handle("id", LocalDate.now(ZoneId.systemDefault()), "Failed to write", null);
        verify(dateRangeIndex, never()).extendRange(any(), any());
    }

    @Test
    public void kwInDeletesInstanceOfMissingReadings() throws BlobStoreException {
        Readings readings = ZeroedReadingsCache.ofMissing(1);

        setupBlobWriter(false, false, true);

        assertTrue(profileWriter.writeKwIn("id", LocalDate.now(ZoneId.systemDefault()), readings, onError));

        verify(blobWriter).delete("id", KW_IN.storeString());
        verify(onError, never()).handle(any(), any(), any(), any());
    }

    @Test
    public void writesKwOut() throws BlobStoreException {
        Readings readings = Readings.of(Channel.of(1.));

        setupBlobWriter(true, false, false);

        assertTrue(profileWriter.writeKwOut("id", LocalDate.now(ZoneId.systemDefault()), readings, onError));
        verify(blobWriter).write("id", KW_OUT.storeString(), kwOutBytes, 0, kwOutBytes.length);

        verify(onError, never()).handle(any(), any(), any(), any());
        verify(dateRangeIndex).extendRange(id, date);
    }

    @Test
    public void updatesKwOut() throws BlobStoreException {
        Readings readings = Readings.of(Channel.of(1.));

        setupBlobWriter(false, true, false);

        assertTrue(profileWriter.writeKwOut("id", LocalDate.now(ZoneId.systemDefault()), readings, onError));
        verify(blobWriter).update("id", KW_OUT.storeString(), kwOutBytes, 0, kwOutBytes.length);

        verify(onError, never()).handle(any(), any(), any(), any());
        verify(dateRangeIndex).extendRange(id, date);
    }

    @Test
    public void writeKwOutDeletesStats() throws Exception {
        Readings readings = Readings.of(Channel.of(1.));

        setupBlobWriter(true, false, true);

        assertTrue(profileWriter.writeKwOut("id", LocalDate.now(ZoneId.systemDefault()), readings, onError));
        verify(blobWriter).delete("id", MAXIMUMS.storeString());
    }

    @Test
    public void kwOutError() throws BlobStoreException {
        Readings readings = Readings.of(Channel.of(1.));

        setupBlobWriter(false, false, false);

        assertFalse(profileWriter.writeKwOut("id", LocalDate.now(ZoneId.systemDefault()), readings, onError));
        verify(blobWriter).write("id", KW_OUT.storeString(), kwOutBytes, 0, kwOutBytes.length);
        verify(blobWriter).update("id", KW_OUT.storeString(), kwOutBytes, 0, kwOutBytes.length);
        verify(onError).handle("id", LocalDate.now(ZoneId.systemDefault()), "Failed to write", null);
        verify(dateRangeIndex, never()).extendRange(any(), any());
    }

    @Test
    public void kwOutDeletesInstanceOfMissingReadings() throws BlobStoreException {
        Readings readings = ZeroedReadingsCache.ofMissing(1);

        setupBlobWriter(false, false, true);

        assertTrue(profileWriter.writeKwOut("id", LocalDate.now(ZoneId.systemDefault()), readings, onError));

        verify(blobWriter).delete("id", KW_OUT.storeString());
        verify(onError, never()).handle(any(), any(), any(), any());
    }

    @Test
    public void writesCacheable() throws BlobStoreException {
        setupBlobWriter(true, false, false);

        assertTrue(profileWriter.writeCacheable("id", LocalDate.now(ZoneId.systemDefault()), true, onError));
        verify(blobWriter).write("id", CACHEABLE.storeString(), cacheableBytes, 0, cacheableBytes.length);

        verify(onError, never()).handle(any(), any(), any(), any());
        verify(dateRangeIndex).extendRange(id, date);
    }

    @Test
    public void updatesCacheable() throws BlobStoreException {
        setupBlobWriter(false, false, true);

        assertTrue(profileWriter.writeCacheable("id", LocalDate.now(ZoneId.systemDefault()), false, onError));
        verify(blobWriter).delete("id", CACHEABLE.storeString());

        verify(onError, never()).handle(any(), any(), any(), any());
        verify(dateRangeIndex).extendRange(id, date);
    }

    @Test
    public void cacheableError() throws BlobStoreException {
        setupBlobWriter(false, false, false);

        assertFalse(profileWriter.writeCacheable("id", LocalDate.now(ZoneId.systemDefault()), true, onError));
        verify(blobWriter).write("id", CACHEABLE.storeString(), new byte[]{1}, 0, 1);
        verify(onError).handle("id", LocalDate.now(ZoneId.systemDefault()), "Failed to write", null);
        verify(dateRangeIndex, never()).extendRange(any(), any());
    }

    @Test
    public void writeHandlesBlobstoreError() throws BlobStoreException {
        EnergyProfile profile = newProfile(id, date, true);

        setupBlobWriter(false, false, false);

        profileWriter.write(profile, onError);
        verify(onError).handle("id", LocalDate.now(ZoneId.systemDefault()), "Failed to write", null);
    }

    @Test
    public void notifiesErrorWhenIndexFailsToUpdate() throws Exception {
        EnergyProfile profile = newProfile(id, date, true);
        setupBlobWriter(true, false, false);
        doReturn(false).when(dateRangeIndex).save(any(), any(), any());

        profileWriter.write(profile, onError);
        verify(onError).handle(id, date, "Unable to extend date range in index", null);
    }

    @Test
    public void commits() {
        assertTrue(profileWriter.commit(onError));
        verify(byDateItemWriter).commit(any());
        verify(dateRangeIndex).commit();
    }

    @Test
    public void commitError() {
        doReturn(false).when(byDateItemWriter).commit(any());
        assertFalse(profileWriter.commit(onError));
        verify(dateRangeIndex, never()).commit();
    }

    @Test
    public void rollsback() {
        assertTrue(profileWriter.rollback(onError));
        verify(byDateItemWriter).rollback(any());
        verify(dateRangeIndex).rollback();
    }

    @Test
    public void rollbackError() {
        doReturn(false).when(byDateItemWriter).rollback(any());
        assertFalse(profileWriter.rollback(onError));
        verify(dateRangeIndex, never()).commit();
    }

}
