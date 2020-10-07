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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import static com.zepben.energy.datastore.blobstore.indexing.BlobDateRangeIndex.STORE_TAG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class BlobDateRangeIndexTest {

    @EverythingIsNonnullByDefault
    class MockBlobStore implements BlobStore {

        @Override
        public BlobReader reader() {
            return blobReader;
        }

        @Override
        public BlobWriter writer() {
            return blobWriter;
        }

        @Override
        public void close() {

        }

    }

    private BlobReader blobReader = mock(BlobReader.class);
    private BlobWriter blobWriter = mock(BlobWriter.class);
    private BlobStore blobStore = spy(new MockBlobStore());
    private BlobDateRangeIndex index = new BlobDateRangeIndex(blobStore);
    @Mock private Consumer<IdDateRange> handler;
    @Captor private ArgumentCaptor<BlobReader.BlobHandler> blobHandlerCaptor;

    private String id = "id";
    private LocalDate from = LocalDate.now();
    private LocalDate to = from.plusDays(10);
    private IdDateRange expectedRange = new IdDateRange(id, from, to);
    private byte[] rangeBytes = new IdDateRangeCodec().serialise(from, to);

    @BeforeEach
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void closesBlobStre() throws Exception {
        index.close();
        verify(blobStore).close();
    }

    @Test
    public void get() throws Exception {
        doReturn(rangeBytes).when(blobReader).get(id, STORE_TAG);
        IdDateRange range = index.get(id);
        assertThat(range, equalTo(expectedRange));
    }

    @Test
    public void forEach() throws Exception {
        Collection<String> ids = Collections.singleton(id);
        index.forEach(ids, handler);

        verify(blobReader).forEach(eq(ids), eq(STORE_TAG), blobHandlerCaptor.capture());
        blobHandlerCaptor.getValue().handle(id, STORE_TAG, rangeBytes);
        verify(handler).accept(expectedRange);
    }

    @Test
    public void forAll() throws Exception {
        index.forAll(handler);

        verify(blobReader).forAll(eq(STORE_TAG), blobHandlerCaptor.capture());
        blobHandlerCaptor.getValue().handle(id, STORE_TAG, rangeBytes);
        verify(handler).accept(expectedRange);
    }

    @Test
    public void saveWrites() throws Exception {
        doReturn(false).when(blobWriter).update(any(), any(), any());
        doReturn(true).when(blobWriter).write(any(), any(), any());
        assertThat(index.save(id, from, to), is(true));
        verify(blobWriter).write(id, STORE_TAG, rangeBytes);
    }

    @Test
    public void saveUpdates() throws Exception {
        doReturn(true).when(blobWriter).update(any(), any(), any());
        assertThat(index.save(id, from, to), is(true));
        verify(blobWriter).update(id, STORE_TAG, rangeBytes);
    }

    @Test
    public void saveReturnsFalseOnException() throws Exception {
        doThrow(new BlobStoreException("test", null)).when(blobWriter).update(any(), any(), any());
        assertThat(index.save(id, from, to), is(false));
    }

    @Test
    public void saveReturnsFalseWhenWriteAndUpdateFail() {
        assertThat(index.save(id, from, to), is(false));
    }

    @Test
    public void updatesFrom() throws Exception {
        index = spy(index);
        doReturn(expectedRange).when(index).get(id);
        doReturn(true).when(blobWriter).update(any(), any(), any());

        LocalDate newFrom = from.plusDays(1);
        assertThat(index.saveFrom(id, newFrom), is(true));
        verify(index).save(id, newFrom, to);
    }

    @Test
    public void doesNotUpdateFromWhenInvalidRange() {
        index = spy(index);
        doReturn(expectedRange).when(index).get(id);

        LocalDate newFrom = to.plusDays(1);
        assertThat(index.saveFrom(id, newFrom), is(false));
        verify(index, never()).save(any(), any(), any());
    }

    @Test
    public void doesNotUpdateFromWhenUnknownId() {
        index = spy(index);

        LocalDate newFrom = to.plusDays(1);
        assertThat(index.saveFrom("unknown", newFrom), is(false));
        verify(index, never()).save(any(), any(), any());
    }

    @Test
    public void updatesTo() throws Exception {
        index = spy(index);
        doReturn(expectedRange).when(index).get(id);
        doReturn(true).when(blobWriter).update(any(), any(), any());

        LocalDate newTo = to.plusDays(1);
        assertThat(index.saveTo(id, newTo), is(true));
        verify(index).save(id, from, newTo);
    }

    @Test
    public void doesNotUpdateToWhenInvalidRange() {
        index = spy(index);
        doReturn(expectedRange).when(index).get(id);

        LocalDate newTo = from.minusDays(1);
        assertThat(index.saveTo(id, newTo), is(false));
        verify(index, never()).save(any(), any(), any());
    }

    @Test
    public void doesNotUpdateToWhenUnknownId() {
        index = spy(index);

        LocalDate newTo = from.minusDays(1);
        assertThat(index.saveTo("unknown", newTo), is(false));
        verify(index, never()).save(any(), any(), any());
    }

    @Test
    public void commits() throws Exception {
        index.commit();
        verify(blobWriter).commit();
    }

    @Test
    public void rollsback() throws Exception {
        index.rollback();
        verify(blobWriter).rollback();
    }

}
