/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.datastore.blobstore;

import com.zepben.blobstore.itemwrappers.ByDateItemError;
import com.zepben.blobstore.itemwrappers.ByDateItemHandler;
import com.zepben.blobstore.itemwrappers.ByDateItemReader;
import com.zepben.energy.datastore.ErrorHandler;
import com.zepben.energy.datastore.ItemHandler;
import com.zepben.energy.datastore.blobstore.indexing.DateRangeTest;
import com.zepben.energy.model.Channel;
import com.zepben.energy.model.EnergyProfile;
import com.zepben.energy.model.Readings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ByDatePartialProfileReaderTest {

    @Mock private ByDateItemReader<EnergyProfile> itemReader;
    @Mock private DateRangeTest dateRangeTest;
    private EnergyProfileAttribute tag = EnergyProfileAttribute.KW_IN;
    private ByDatePartialProfileReader<Readings> reader;
    @Mock private ItemHandler<Readings> itemHandler;
    @Captor private ArgumentCaptor<ByDateItemHandler<Readings>> itemHandlerCaptor;
    @Mock private ErrorHandler onError;
    @Captor private ArgumentCaptor<ByDateItemError> onErrorCaptor;
    private LocalDate date = LocalDate.now();

    @BeforeEach
    public void before() {
        MockitoAnnotations.initMocks(this);
        doReturn(true).when(dateRangeTest).idHasDate(any(), any());
        doAnswer(inv -> inv.getArgument(0)).when(dateRangeTest).filterIdsWithDate(any(), any());
        reader = new ByDatePartialProfileReader<>(tag, itemReader, dateRangeTest);
    }

    @Test
    public void correctTag() {
        assertThat(reader.tag(), is(tag));
    }

    @Test
    public void get() {
        String id = "id";

        reader.get(id, date, onError);
        verify(itemReader).get(eq(id), eq(date), eq(tag.storeString()), onErrorCaptor.capture());
        onErrorCaptor.getValue().handle(id, date, "test", null);
        verify(onError).handle(id, date, "test", null);
    }

    @Test
    public void getIdNotInDate() {
        String id = "id";

        doReturn(false).when(dateRangeTest).idHasDate(any(), any());
        reader.get(id, date, onError);
        verify(itemReader, never()).get(any(), any(), any());
    }

    @Test
    public void forEach() {
        Collection<String> ids = Arrays.asList("id1", "id2");
        reader.forEach(ids, date, itemHandler, onError);
        verify(itemReader).forEach(eq(ids), eq(date), eq(tag.storeString()), itemHandlerCaptor.capture(), onErrorCaptor.capture());

        Readings readings = Readings.of(Channel.of(1));
        itemHandlerCaptor.getValue().handle("id1", date, readings);
        verify(itemHandler).handle("id1", date, readings);

        onErrorCaptor.getValue().handle("id1", date, "test", null);
        verify(onError).handle("id1", date, "test", null);
    }

    @Test
    public void forEachFiltersIdsWhenNotInDate() throws Exception {
        Collection<String> ids = Arrays.asList("id1", "id2");
        doReturn(Collections.singletonList("id2")).when(dateRangeTest).filterIdsWithDate(ids, date);
        reader.forEach(ids, date, itemHandler, onError);
        verify(itemReader).forEach(eq(Collections.singletonList("id2")), eq(date), eq(tag.storeString()), any(), any());
    }

    @Test
    public void forEachShortCircuitsWhenNoneIdDate() throws Exception {
        Collection<String> ids = Arrays.asList("id1", "id2");
        doReturn(Collections.emptyList()).when(dateRangeTest).filterIdsWithDate(ids, date);
        reader.forEach(ids, date, itemHandler, onError);
        verify(itemReader, never()).forEach(any(), any(), any(), any(), any());
    }

    @Test
    public void forAll() {
        reader.forAll(date, itemHandler, onError);
        verify(itemReader).forAll(eq(date), eq(tag.storeString()), itemHandlerCaptor.capture(), onErrorCaptor.capture());

        Readings readings = Readings.of(Channel.of(1));
        itemHandlerCaptor.getValue().handle("id1", date, readings);
        verify(itemHandler).handle("id1", date, readings);

        onErrorCaptor.getValue().handle("id1", date, "test", null);
        verify(onError).handle("id1", date, "test", null);
    }

}