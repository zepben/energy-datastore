/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.ewb.datastores.energy;

import com.zepben.blobstore.BlobStore;
import com.zepben.blobstore.BlobStoreException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static com.zepben.testutils.exception.ExpectException.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ByDateBlobStoreCacheTest {

    private LocalDate date = LocalDate.now();
    private ZoneId timeZone = ZoneId.systemDefault();

    @Test
    public void doesCache() throws Exception {
        ByDateBlobStoreProvider provider = mock(ByDateBlobStoreProvider.class);
        when(provider.get(any(), any(), anyBoolean())).thenReturn(mock(BlobStore.class, RETURNS_MOCKS));
        ByDateBlobStoreCache cache = new ByDateBlobStoreCache(provider);

        cache.getReader(date, timeZone);
        cache.getWriter(date, timeZone);
        cache.getReader(date, timeZone);
        cache.getWriter(date.minusDays(1), timeZone);

        verify(provider, times(1)).get(date, timeZone, false);
        verify(provider, times(1)).get(date.minusDays(1), timeZone, true);

        ByDateBlobStoreCache.ErrorHandler errorHandler = mock(ByDateBlobStoreCache.ErrorHandler.class);
        cache.close(errorHandler);
        verify(errorHandler, never()).handle(any(), any(), any());
    }

    @Test
    public void callsBackOnCloseException() throws Exception {
        ByDateBlobStoreProvider provider = mock(ByDateBlobStoreProvider.class);
        BlobStore blobStore = mock(BlobStore.class, RETURNS_MOCKS);
        when(provider.get(any(), any(), anyBoolean())).thenReturn(blobStore);
        ByDateBlobStoreCache cache = new ByDateBlobStoreCache(provider);

        cache.getReader(date, timeZone);

        BlobStoreException expectedExcetion = new BlobStoreException("test", null);
        doThrow(expectedExcetion).when(blobStore).close();
        ByDateBlobStoreCache.ErrorHandler errorHandler = mock(ByDateBlobStoreCache.ErrorHandler.class);
        cache.close(errorHandler);
        verify(errorHandler).handle(eq(blobStore), eq(date), eq(expectedExcetion));
    }

    @Test
    public void getWriterThrowsOnNullProvidedBlobStore() {
        ByDateBlobStoreProvider provider = mock(ByDateBlobStoreProvider.class);
        ByDateBlobStoreCache cache = new ByDateBlobStoreCache(provider);
        expect(() -> cache.getWriter(date, timeZone)).toThrow(IllegalStateException.class);
    }

    @Test
    public void getReaderReturnsNullOnNullProvidedBlobStore() throws Exception {
        ByDateBlobStoreProvider provider = mock(ByDateBlobStoreProvider.class);
        ByDateBlobStoreCache cache = new ByDateBlobStoreCache(provider);
        assertThat(cache.getReader(date, timeZone), is(nullValue()));
    }

    @Test
    public void doesNotCacheNullStore() throws Exception {
        ByDateBlobStoreProvider provider = mock(ByDateBlobStoreProvider.class);
        ByDateBlobStoreCache cache = new ByDateBlobStoreCache(provider);
        assertThat(cache.getReader(date, timeZone), is(nullValue()));

        // Close will close all the cached stores
        cache.close(mock(ByDateBlobStoreCache.ErrorHandler.class));
    }

}
