/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.datastore;

import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.energy.model.EnergyProfile;
import com.zepben.energy.model.EnergyProfileStat;
import com.zepben.energy.model.IdDateRange;
import com.zepben.energy.model.Readings;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.Collection;
import java.util.function.Consumer;

@EverythingIsNonnullByDefault
public interface EnergyProfileReader {

    @Nullable
    IdDateRange getDateRange(String id);

    void forEachGetDateRange(Collection<String> ids, Consumer<IdDateRange> handler);

    void forAllGetDateRange(Consumer<IdDateRange> handler);

    @Nullable
    EnergyProfile get(String id,
                      LocalDate date,
                      ErrorHandler onError);

    void forEach(Collection<String> ids,
                 LocalDate date,
                 ItemHandler<EnergyProfile> onRead,
                 ErrorHandler onError);

    void forAll(LocalDate date,
                ItemHandler<EnergyProfile> onRead,
                ErrorHandler onError);

    default void forAllCacheable(LocalDate date,
                                 ItemHandler<EnergyProfile> onRead,
                                 ErrorHandler onError) {
        ItemHandler<EnergyProfile> onReadCacheable = (id, dt, profile) -> {
            if (profile.cacheable())
                onRead.handle(id, dt, profile);
        };

        forAll(date, onReadCacheable, onError);
    }

    PartialProfileReader<Readings> kwInReader();

    PartialProfileReader<Readings> kwOutReader();

    PartialProfileReader<Boolean> isCacheableReader();

    PartialProfileReader<EnergyProfileStat> maximumsReader();

}
