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
import kotlin.Unit;
import kotlin.jvm.functions.Function3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import static com.zepben.energy.datastore.blobstore.indexing.BlobDateRangeIndex.STORE_TAG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class BlobDateRangeIndexTest {

    @SuppressWarnings("NullableProblems")
    @EverythingIsNonnullByDefault
    class MockBlobStore implements BlobStore {

        @Override
        public BlobReader getReader() {
            return blobReader;
        }

        @Override
        public BlobWriter getWriter() {
            return blobWriter;
        }

        @Override
        public void close() {

        }

    }

    private final BlobReader blobReader = mock(BlobReader.class);
    private final BlobWriter blobWriter = mock(BlobWriter.class);
    private final BlobStore blobStore = spy(new MockBlobStore());
    private BlobDateRangeIndex index = new BlobDateRangeIndex(blobStore);
    @Mock private Consumer<IdDateRange> handler;
    @Captor private ArgumentCaptor<Function3<String, String, byte[], Unit>> blobHandlerCaptor;

    private final String id = "id";
    private final LocalDate from = LocalDate.now(ZoneId.systemDefault());
    private final LocalDate to = from.plusDays(10);
    private final IdDateRange expectedRange = new IdDateRange(id, from, to);
    private final byte[] rangeBytes = new IdDateRangeCodec().serialise(from, to);

    @BeforeEach
    public void before() throws Exception {
        MockitoAnnotations.openMocks(this).close();
    }

    @Test
    public void closesBlobStore() throws Exception {
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
        blobHandlerCaptor.getValue().invoke(id, STORE_TAG, rangeBytes);
        verify(handler).accept(expectedRange);
    }

    @Test
    public void forAll() throws Exception {
        index.forAll(handler);

        verify(blobReader).forAll(eq(STORE_TAG), blobHandlerCaptor.capture());
        blobHandlerCaptor.getValue().invoke(id, STORE_TAG, rangeBytes);
        verify(handler).accept(expectedRange);
    }

    @Test
    public void saveWrites() throws Exception {
        doReturn(false).when(blobWriter).update(any(), any(), any(), anyInt(), anyInt());
        doReturn(true).when(blobWriter).write(any(), any(), any(), anyInt(), anyInt());
        assertThat(index.save(id, from, to), is(true));
        verify(blobWriter).write(id, STORE_TAG, rangeBytes, 0, rangeBytes.length);
    }

    @Test
    public void saveUpdates() throws Exception {
        doReturn(true).when(blobWriter).update(any(), any(), any(), anyInt(), anyInt());
        assertThat(index.save(id, from, to), is(true));
        verify(blobWriter).update(id, STORE_TAG, rangeBytes, 0, rangeBytes.length);
    }

    @Test
    public void saveReturnsFalseOnException() throws Exception {
        doThrow(new BlobStoreException("test", null)).when(blobWriter).update(any(), any(), any(), anyInt(), anyInt());
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
        doReturn(true).when(blobWriter).update(any(), any(), any(), anyInt(), anyInt());

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
        doReturn(true).when(blobWriter).update(any(), any(), any(), anyInt(), anyInt());

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
