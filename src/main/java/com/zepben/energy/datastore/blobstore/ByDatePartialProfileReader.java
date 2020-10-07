/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.datastore.blobstore;

import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.blobstore.itemwrappers.ByDateItemReader;
import com.zepben.energy.datastore.ErrorHandler;
import com.zepben.energy.datastore.ItemHandler;
import com.zepben.energy.datastore.PartialProfileReader;
import com.zepben.energy.datastore.blobstore.indexing.DateRangeTest;
import com.zepben.energy.model.EnergyProfile;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.Collection;

@EverythingIsNonnullByDefault
public class ByDatePartialProfileReader<T> implements PartialProfileReader<T> {

    private final EnergyProfileAttribute tag;
    private final ByDateItemReader<EnergyProfile> itemReader;
    private final DateRangeTest dateRangeTest;

    ByDatePartialProfileReader(EnergyProfileAttribute tag,
                               ByDateItemReader<EnergyProfile> itemReader,
                               DateRangeTest dateRangeTest) {
        this.tag = tag;
        this.itemReader = itemReader;
        this.dateRangeTest = dateRangeTest;
    }

    @SuppressWarnings("WeakerAccess")
    public EnergyProfileAttribute tag() {
        return tag;
    }

    ByDateItemReader<EnergyProfile> itemReader() {
        return itemReader;
    }

    @Override
    @Nullable
    public T get(String id, LocalDate date, ErrorHandler onError) {
        if (!dateRangeTest.idHasDate(id, date))
            return null;

        return itemReader.get(id, date, tag.storeString(), onError::handle);
    }

    @Override
    public void forEach(Collection<String> ids,
                        LocalDate date,
                        ItemHandler<T> onRead,
                        ErrorHandler onError) {
        Collection<String> validIds = dateRangeTest.filterIdsWithDate(ids, date);
        if (validIds.isEmpty())
            return;

        itemReader.forEach(validIds, date, tag.storeString(), onRead::handle, onError::handle);
    }

    @Override
    public void forAll(LocalDate date,
                       ItemHandler<T> onRead,
                       ErrorHandler onError) {
        itemReader.forAll(date, tag.storeString(), onRead::handle, onError::handle);
    }

}
