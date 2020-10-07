/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.datastore.blobstore;

import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.energy.datastore.ErrorHandler;
import com.zepben.energy.datastore.ItemHandler;
import com.zepben.energy.datastore.PartialProfileReader;
import com.zepben.energy.model.EnergyProfile;
import com.zepben.energy.model.EnergyProfileStat;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * Reader that attempts to read pre-calculated stats from the appropriate table
 * and creates them on the fly if the cached stat for the profile does not exist.
 */
@EverythingIsNonnullByDefault
class EnergyProfileStatReader implements PartialProfileReader<EnergyProfileStat> {

    private final ByDatePartialProfileReader<EnergyProfileStat> reader;
    private final Function<EnergyProfile, EnergyProfileStat> statFactory;

    EnergyProfileStatReader(ByDatePartialProfileReader<EnergyProfileStat> reader, Function<EnergyProfile, EnergyProfileStat> statFactory) {
        this.reader = reader;
        this.statFactory = statFactory;
    }

    // Package private for testing... Can we do this better?
    ByDatePartialProfileReader<EnergyProfileStat> reader() {
        return reader;
    }

    // Package private for testing... Can we do this better?
    Function<EnergyProfile, EnergyProfileStat> statFactory() {
        return statFactory;
    }

    @Nullable
    @Override
    public EnergyProfileStat get(String id, LocalDate date, ErrorHandler onError) {
        EnergyProfileStat stat = reader.get(id, date, onError);
        if (stat == null) {
            stat = createStat(reader.itemReader().get(id, date, onError::handle));
        }
        return stat;
    }

    @Override
    public void forEach(Collection<String> ids, LocalDate date, ItemHandler<EnergyProfileStat> onRead, ErrorHandler onError) {
        Set<String> trackIds = new HashSet<>(ids);

        ItemHandler<EnergyProfileStat> trackHandler = (id, dt, stat) -> {
            trackIds.remove(id);
            onRead.handle(id, dt, stat);
        };

        reader.forEach(ids, date, trackHandler, onError);

        if (!trackIds.isEmpty()) {
            reader.itemReader().forEach(trackIds, date, createStatHandler(onRead)::handle, onError::handle);
        }
    }

    @Override
    public void forAll(LocalDate date, ItemHandler<EnergyProfileStat> onRead, ErrorHandler onError) {
        // Currently there is no exposed way to find out what's missing from the stat table but exists in a readings table.
        // As this is the case we just do a forAll on every profile.
        reader.itemReader().forAll(date, createStatHandler(onRead)::handle, onError::handle);
    }

    @Nullable
    private EnergyProfileStat createStat(@Nullable EnergyProfile profile) {
        if (profile == null)
            return null;

        return statFactory.apply(profile);
    }

    private ItemHandler<EnergyProfile> createStatHandler(ItemHandler<EnergyProfileStat> onRead) {
        return (id, dt, profile) -> {
            EnergyProfileStat stat = createStat(profile);
            if (stat != null)
                onRead.handle(id, dt, stat);
        };
    }

}
