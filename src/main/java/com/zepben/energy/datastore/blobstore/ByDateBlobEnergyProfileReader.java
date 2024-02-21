/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.datastore.blobstore;

import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.blobstore.WhereBlob;
import com.zepben.blobstore.itemwrappers.*;
import com.zepben.energy.datastore.EnergyProfileReader;
import com.zepben.energy.datastore.ErrorHandler;
import com.zepben.energy.datastore.ItemHandler;
import com.zepben.energy.datastore.blobstore.indexing.DateRangeIndex;
import com.zepben.energy.datastore.blobstore.indexing.DateRangeTest;
import com.zepben.energy.model.EnergyProfile;
import com.zepben.energy.model.EnergyProfileStat;
import com.zepben.energy.model.IdDateRange;
import com.zepben.energy.model.Readings;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import static com.zepben.energy.datastore.blobstore.EnergyProfileAttribute.*;
import static java.util.stream.Collectors.toMap;

@EverythingIsNonnullByDefault
public class ByDateBlobEnergyProfileReader implements EnergyProfileReader {

    private final DateRangeIndex dateRangeIndex;
    private final DateRangeTest dateRangeTest;
    private final ByDateItemReader<EnergyProfile> itemReader;
    private final ByDatePartialProfileReader<Readings> kwInReader;
    private final ByDatePartialProfileReader<Readings> kwOutReader;
    private final ByDatePartialProfileReader<Boolean> isCacheableReader;
    private final EnergyProfileStatReader maximumsReader;

    private final EnergyProfileFactory profileFactory;
    private final Deserialisers dsx;

    private final ByDateItemDeserialiser<EnergyProfile> itemDeserialiser = this::deserialiseItem;
    private final Map<String, ByDateTagDeserialiser<?>> tagDeserialisers = Arrays.stream(EnergyProfileAttribute.values())
        .collect(toMap(EnergyProfileAttribute::storeString, this::tagDeserialiser));

    // NOTE: Hard coding this byte here breaks the generic serialisation / deserialisation offered by the Deserialisers class.
    //       Need to think about how to deal with the issue later.
    final WhereBlob cacheableWhere = WhereBlob.Companion.equals(EnergyProfileAttribute.CACHEABLE.storeString(), new byte[]{1});

    public ByDateBlobEnergyProfileReader(DateRangeIndex dateRangeIndex,
                                         ByDateItemReader<EnergyProfile> itemReader,
                                         EnergyProfileFactory profileFactory,
                                         Deserialisers deserialisers) {
        this.dateRangeIndex = dateRangeIndex;
        this.dateRangeTest = new DateRangeTest(dateRangeIndex);
        this.profileFactory = profileFactory;
        this.dsx = deserialisers;

        this.itemReader = itemReader;
        this.itemReader.setDeserialisers(itemDeserialiser, tagDeserialisers);

        kwInReader = new ByDatePartialProfileReader<>(KW_IN, itemReader, dateRangeTest);
        kwOutReader = new ByDatePartialProfileReader<>(KW_OUT, itemReader, dateRangeTest);
        isCacheableReader = new ByDatePartialProfileReader<>(CACHEABLE, itemReader, dateRangeTest);
        maximumsReader = new EnergyProfileStatReader(
            new ByDatePartialProfileReader<>(MAXIMUMS, itemReader, dateRangeTest),
            EnergyProfileStat::ofMax);
    }

    // Package private for easier testing. A bit clunky, but I just don't have time right now... GMC
    ByDateItemDeserialiser<EnergyProfile> itemDeserialiser() {
        return itemDeserialiser;
    }

    // Package private for easier testing. A bit clunky, but I just don't have time right now... GMC
    Map<String, ByDateTagDeserialiser<?>> tagDeserialisers() {
        return Collections.unmodifiableMap(tagDeserialisers);
    }

    @Nullable
    @Override
    public IdDateRange getDateRange(String id) {
        return dateRangeIndex.get(id);
    }

    @Override
    public void forEachGetDateRange(Collection<String> ids, Consumer<IdDateRange> handler) {
        dateRangeIndex.forEach(ids, handler);
    }

    @Override
    public void forAllGetDateRange(Consumer<IdDateRange> handler) {
        dateRangeIndex.forAll(handler);
    }

    @Nullable
    @Override
    public EnergyProfile get(String id, LocalDate date, ErrorHandler onError) {
        if (!dateRangeTest.idHasDate(id, date))
            return null;

        return itemReader.get(id, date, onError::handle);
    }

    @Override
    public void forEach(Collection<String> ids, LocalDate date, ItemHandler<EnergyProfile> onRead, ErrorHandler onError) {
        Collection<String> validIds = dateRangeTest.filterIdsWithDate(ids, date);
        if (validIds.isEmpty())
            return;

        itemReader.forEach(validIds, date, onRead::handle, onError::handle);
    }

    @Override
    public void forAll(LocalDate date, ItemHandler<EnergyProfile> onRead, ErrorHandler onError) {
        ByDateItemHandler<EnergyProfile> handler = (id, dt, profile) -> {
            if (dateRangeTest.idHasDate(id, dt))
                onRead.handle(id, date, profile);
        };

        itemReader.forAll(date, handler, onError::handle);
    }

    @Override
    public void forAllCacheable(LocalDate date, ItemHandler<EnergyProfile> onRead, ErrorHandler onError) {
        ByDateItemHandler<EnergyProfile> handler = (id, dt, profile) -> {
            if (dateRangeTest.idHasDate(id, dt))
                onRead.handle(id, date, profile);
        };

        itemReader.forAll(date, Collections.singletonList(cacheableWhere), handler, onError::handle);
    }

    @Override
    public ByDatePartialProfileReader<Readings> kwInReader() {
        return kwInReader;
    }

    @Override
    public ByDatePartialProfileReader<Readings> kwOutReader() {
        return kwOutReader;
    }

    @Override
    public ByDatePartialProfileReader<Boolean> isCacheableReader() {
        return isCacheableReader;
    }

    @Override
    public EnergyProfileStatReader maximumsReader() {
        return maximumsReader;
    }

    private ByDateTagDeserialiser<?> tagDeserialiser(EnergyProfileAttribute attr) {
        switch (attr) {
            case KW_IN:
                return (id, date, tag, blob) -> dsx.kwInDsx().dsx(blob);
            case KW_OUT:
                return (id, date, tag, blob) -> dsx.kwOutDsx().dsx(blob);
            case CACHEABLE:
                return (id, date, tag, blob) -> dsx.cacheableDsx().dsx(blob);
            case MAXIMUMS:
                return (id, date, tag, blob) -> dsx.statDsx().dsx(blob);
            default:
                throw new AssertionError("Internal error: Missing deserialise case for tag " + attr);
        }
    }

    private EnergyProfile deserialiseItem(String id,
                                          LocalDate date,
                                          Map<String, byte[]> blobs) throws DeserialiseException {
        Readings kwIn = deserialiseReadings(KW_IN, blobs, dsx.kwInDsx());
        Readings kwOut = deserialiseReadings(KW_OUT, blobs, dsx.kwOutDsx());
        boolean cacheable = deserialiseCacheable(blobs);
        return profileFactory.create(id, date, kwIn, kwOut, cacheable);
    }

    @Nullable
    private Readings deserialiseReadings(EnergyProfileAttribute tag,
                                         Map<String, byte[]> blobs,
                                         Deserialiser<Readings> dsx) throws DeserialiseException {
        byte[] bytes = blobs.get(tag.storeString());
        if (bytes == null)
            return null;

        Readings readings = dsx.dsx(bytes);
        if (readings == null) {
            throw new DeserialiseException("failed to deserialise readings: " + tag.storeString(), null);
        }

        return readings;
    }

    private boolean deserialiseCacheable(Map<String, byte[]> blobs) {
        byte[] bytes = blobs.get(CACHEABLE.storeString());
        if (bytes == null)
            return false;

        Boolean result = dsx.cacheableDsx().dsx(bytes);
        return result != null && result;
    }

}
