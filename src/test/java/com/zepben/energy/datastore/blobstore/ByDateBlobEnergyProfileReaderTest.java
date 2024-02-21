/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.datastore.blobstore;

import com.zepben.blobstore.itemwrappers.*;
import com.zepben.energy.datastore.ErrorHandler;
import com.zepben.energy.datastore.ItemHandler;
import com.zepben.energy.datastore.blobstore.indexing.DateRangeIndex;
import com.zepben.energy.datastore.blobstore.indexing.MockDateRangeIndex;
import com.zepben.energy.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Consumer;

import static com.zepben.energy.datastore.blobstore.EnergyProfileAttribute.*;
import static com.zepben.testutils.exception.ExpectException.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ByDateBlobEnergyProfileReaderTest {

    private final String id = "id";
    private final LocalDate date = LocalDate.now(ZoneId.systemDefault());

    private final DateRangeIndex dateRangeIndex = spy(new MockDateRangeIndex(Collections.singletonList(new IdDateRange(id, date, date))));
    @Mock private ByDateItemReader<EnergyProfile> byDateItemReader;
    @Mock private Deserialiser<Readings> kwInDsx;
    @Mock private Deserialiser<Readings> kwOutDsx;
    @Mock private Deserialiser<Boolean> cacheableDsx;
    @Mock private Deserialiser<EnergyProfileStat> statDsx;
    @Mock private ItemHandler<EnergyProfile> itemHandler;
    @Mock private ErrorHandler itemError;

    @Captor private ArgumentCaptor<Map<String, ByDateTagDeserialiser<?>>> tagsDeserialiserMapCaptor;
    @Captor private ArgumentCaptor<ByDateItemHandler<EnergyProfile>> byDateItemHandlerCaptor;
    @Captor private ArgumentCaptor<ByDateItemError> byDateItemErrorCaptor;

    private ByDateBlobEnergyProfileReader profileReader;

    @BeforeEach
    public void before() throws Exception {
        MockitoAnnotations.openMocks(this).close();

        Deserialisers deserialisers = new Deserialisers(kwInDsx, kwOutDsx, cacheableDsx, statDsx);
        profileReader = new ByDateBlobEnergyProfileReader(dateRangeIndex, byDateItemReader, EnergyProfile::of, deserialisers);
    }

    @Test
    public void registersDeserialisers() {
        verify(byDateItemReader).setDeserialisers(profileReader.itemDeserialiser(), profileReader.tagDeserialisers());
    }

    @Test
    public void registersAllTagsHandlers() {
        verify(byDateItemReader).setDeserialisers(any(), tagsDeserialiserMapCaptor.capture());
        Map<String, ByDateTagDeserialiser<?>> tagDeserialisers = tagsDeserialiserMapCaptor.getValue();

        Object[] expectedKeys = Arrays.stream(EnergyProfileAttribute.values()).map(EnergyProfileAttribute::storeString).toArray();
        assertThat(tagDeserialisers.keySet(), containsInAnyOrder(expectedKeys));
    }

    @Test
    public void getDateRange() {
        profileReader.getDateRange(id);
        verify(dateRangeIndex).get(id);
    }

    @Test
    public void forEachGetDateRange() {
        @SuppressWarnings("unchecked")
        Consumer<IdDateRange> handler = mock(Consumer.class);

        Collection<String> ids = Arrays.asList("id1", "id2");
        profileReader.forEachGetDateRange(ids, handler);
        verify(dateRangeIndex).forEach(ids, handler);
    }

    @Test
    public void forAllGetDateRange() {
        @SuppressWarnings("unchecked")
        Consumer<IdDateRange> handler = mock(Consumer.class);

        profileReader.forAllGetDateRange(handler);
        verify(dateRangeIndex).forAll(handler);
    }

    @Test
    public void itemDeserialiser() throws Exception {
        ByDateItemDeserialiser<EnergyProfile> deserialiser = profileReader.itemDeserialiser();
        Readings readingsIn = Readings.of(Channel.of(1.));
        Readings readingsOut = Readings.of(Channel.of(2.));
        when(kwInDsx.dsx(any())).thenReturn(readingsIn);
        when(kwOutDsx.dsx(any())).thenReturn(readingsOut);
        when(cacheableDsx.dsx(any())).thenReturn(true);

        Map<String, byte[]> blobs = new HashMap<>();
        blobs.put(KW_IN.storeString(), new byte[]{});
        blobs.put(KW_OUT.storeString(), new byte[]{});
        blobs.put(CACHEABLE.storeString(), new byte[]{});
        EnergyProfile profile = deserialiser.deserialise(id, date, blobs);

        EnergyProfile expectedProfile = EnergyProfile.ofCacheable(id, date, readingsIn, readingsOut);
        assertThat(profile, is(expectedProfile));
    }

    @Test
    public void itemDeserialiserThrowsDeserialiseExceptionOnNullReadings() {
        ByDateItemDeserialiser<EnergyProfile> deserialiser = profileReader.itemDeserialiser();
        when(kwInDsx.dsx(any())).thenReturn(null);

        Map<String, byte[]> blobs = new HashMap<>();
        blobs.put(KW_IN.storeString(), new byte[]{});
        blobs.put(KW_OUT.storeString(), new byte[]{});
        blobs.put(CACHEABLE.storeString(), new byte[]{1});

        expect(() -> deserialiser.deserialise(id, date, blobs)).toThrow(DeserialiseException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void kwInTagDersialiser() throws Exception {
        var deserialiser = (ByDateTagDeserialiser<Readings>)profileReader.tagDeserialisers().get(KW_IN.storeString());

        Readings readingsIn = Readings.of(Channel.of(1.));
        when(kwInDsx.dsx(any())).thenReturn(readingsIn);
        Readings actual = deserialiser.deserialise("", LocalDate.now(ZoneId.systemDefault()), KW_IN.storeString(), new byte[]{});
        assertThat(actual, equalTo(readingsIn));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void kwOutTagDersialiser() throws Exception {
        var deserialiser = (ByDateTagDeserialiser<Readings>)profileReader.tagDeserialisers().get(KW_OUT.storeString());

        Readings readingsOut = Readings.of(Channel.of(1.));
        when(kwOutDsx.dsx(any())).thenReturn(readingsOut);
        Readings actual = deserialiser.deserialise("", LocalDate.now(ZoneId.systemDefault()), KW_OUT.storeString(), new byte[]{});
        assertThat(actual, equalTo(readingsOut));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void isCacheableTagDersialiser() throws Exception {
        var deserialiser = (ByDateTagDeserialiser<Boolean>)profileReader.tagDeserialisers().get(CACHEABLE.storeString());

        when(cacheableDsx.dsx(any())).thenReturn(true);
        Boolean actual = deserialiser.deserialise("", LocalDate.now(ZoneId.systemDefault()), CACHEABLE.storeString(), new byte[]{1});
        assertThat(actual, equalTo(true));
    }

    @Test
    public void get() {
        Readings kwIn = Readings.of(Channel.of(1.));
        Readings kwOut = Readings.of(Channel.of(2.));
        EnergyProfile expectedProfile = EnergyProfile.of(id, date, kwIn, kwOut);

        when(byDateItemReader.get(eq(id), eq(date), any())).thenReturn(expectedProfile);
        EnergyProfile profile = profileReader.get(id, date, itemError);
        assertThat(profile, is(expectedProfile));

        verify(byDateItemReader).get(eq(id), eq(date), byDateItemErrorCaptor.capture());
        byDateItemErrorCaptor.getValue().handle(id, date, "test", null);
        verify(itemError).handle(id, date, "test", null);
    }

    @Test
    public void getNotInDateRange() {
        EnergyProfile profile = profileReader.get(id, date.plusDays(1), itemError);
        assertThat(profile, is(nullValue()));
        verify(byDateItemReader, never()).get(any(), any(), any());
    }

    @Test
    public void forEach() {
        List<String> ids = Collections.singletonList(id);
        Readings kwIn = Readings.of(Channel.of(1.));
        Readings kwOut = Readings.of(Channel.of(2.));
        EnergyProfile expectedProfile = EnergyProfile.of(id, date, kwIn, kwOut);

        profileReader.forEach(ids, date, itemHandler, itemError);
        verify(byDateItemReader).forEach(eq(ids), eq(date), byDateItemHandlerCaptor.capture(), byDateItemErrorCaptor.capture());

        byDateItemHandlerCaptor.getValue().handle(id, date, expectedProfile);
        verify(itemHandler).handle(id, date, expectedProfile);

        byDateItemErrorCaptor.getValue().handle(id, date, "test", null);
        verify(itemError).handle(id, date, "test", null);
    }

    @Test
    public void forEachNoneInDateRange() {
        List<String> ids = Collections.singletonList(id);
        profileReader.forEach(ids, date.plusDays(1), itemHandler, itemError);
        verify(byDateItemReader, never()).forEach(any(), any(), any(), any());
    }

    @Test
    public void forEachPartialInDateRange() {
        List<String> ids = Arrays.asList(id, "missing");
        profileReader.forEach(ids, date, itemHandler, itemError);
        verify(byDateItemReader).forEach(eq(Collections.singletonList(id)), eq(date), any(), any());
    }

    @Test
    public void forAll() {
        Readings kwIn = Readings.of(Channel.of(1.));
        Readings kwOut = Readings.of(Channel.of(2.));
        EnergyProfile expectedProfile = EnergyProfile.of(id, date, kwIn, kwOut);

        profileReader.forAll(date, itemHandler, itemError);
        verify(byDateItemReader).forAll(eq(date), byDateItemHandlerCaptor.capture(), byDateItemErrorCaptor.capture());

        byDateItemHandlerCaptor.getValue().handle(id, date, expectedProfile);
        verify(itemHandler).handle(id, date, expectedProfile);

        byDateItemErrorCaptor.getValue().handle(id, date, "test", null);
        verify(itemError).handle(id, date, "test", null);
    }

    @Test
    public void forAllIgnoresWhenNotInDateIndex() {
        doAnswer(inv -> {
            ByDateItemHandler<EnergyProfile> handler = inv.getArgument(1);
            EnergyProfile profile = EnergyProfile.of(id, date.plusDays(1), null, null);
            handler.handle(profile.id(), profile.date(), profile);
            return null;
        }).when(byDateItemReader).forAll(eq(date.plusDays(1)), any(), any());

        profileReader.forAll(date.plusDays(1), itemHandler, itemError);
        verify(byDateItemReader).forAll(eq(date.plusDays(1)), any(), any());

        verify(itemHandler, never()).handle(any(), any(), any());
    }

    @Test
    public void forAllCacheable() {
        Readings kwIn = Readings.of(Channel.of(1.));
        Readings kwOut = Readings.of(Channel.of(2.));
        EnergyProfile expectedProfile = EnergyProfile.of(id, date, kwIn, kwOut);

        profileReader.forAllCacheable(date, itemHandler, itemError);
        verify(byDateItemReader).forAll(
            eq(date),
            eq(Collections.singletonList(profileReader.cacheableWhere)),
            byDateItemHandlerCaptor.capture(),
            byDateItemErrorCaptor.capture());

        byDateItemHandlerCaptor.getValue().handle(id, date, expectedProfile);
        verify(itemHandler).handle(id, date, expectedProfile);

        byDateItemErrorCaptor.getValue().handle(id, date, "test", null);
        verify(itemError).handle(id, date, "test", null);
    }

    @Test
    public void forAllCacheableIgnoresWhenNotInDateIndex() {
        doAnswer(inv -> {
            ByDateItemHandler<EnergyProfile> handler = inv.getArgument(2);
            EnergyProfile profile = EnergyProfile.of(id, date.plusDays(1), null, null);
            handler.handle(profile.id(), profile.date(), profile);
            return null;
        }).when(byDateItemReader).forAll(eq(date.plusDays(1)), anyList(), any(), any());

        profileReader.forAllCacheable(date.plusDays(1), itemHandler, itemError);
        verify(byDateItemReader).forAll(eq(date.plusDays(1)), anyList(), any(), any());

        verify(itemHandler, never()).handle(any(), any(), any());
    }

    @Test
    public void kwInReader() {
        assertThat(profileReader.kwInReader().tag(), is(KW_IN));
        assertThat(profileReader.kwInReader().itemReader(), is(byDateItemReader));
    }

    @Test
    public void kwOutReader() {
        assertThat(profileReader.kwOutReader().tag(), is(KW_OUT));
        assertThat(profileReader.kwOutReader().itemReader(), is(byDateItemReader));
    }

    @Test
    public void isCacheableReader() {
        assertThat(profileReader.isCacheableReader().tag(), is(CACHEABLE));
        assertThat(profileReader.isCacheableReader().itemReader(), is(byDateItemReader));
    }

    @Test
    public void maximumsReader() {
        assertThat(profileReader.maximumsReader().reader().tag(), is(MAXIMUMS));
        assertThat(profileReader.maximumsReader().reader().itemReader(), is(byDateItemReader));

        EnergyProfile profile = EnergyProfile.of(
            "id",
            LocalDate.now(ZoneId.systemDefault()),
            Readings.of(Channel.of(1, 3, 2)),
            Readings.of(Channel.of(4, 6, 5)));

        EnergyProfileStat stat = profileReader.maximumsReader().statFactory().apply(profile);
        assertThat(stat.kwIn(), is(3.));
        assertThat(stat.kwOut(), is(6.));
        assertThat(stat.kwNet(), is(-3.));
    }

}
