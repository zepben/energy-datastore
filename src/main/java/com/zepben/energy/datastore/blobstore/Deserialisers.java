/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.datastore.blobstore;

import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.energy.model.EnergyProfileStat;
import com.zepben.energy.model.Readings;

@EverythingIsNonnullByDefault
public class Deserialisers {

    private final Deserialiser<Readings> kwInDsx;
    private final Deserialiser<Readings> kwOutDsx;
    private final Deserialiser<Boolean> cacheableDsx;
    private final Deserialiser<EnergyProfileStat> statDsx;

    public Deserialisers(Deserialiser<Readings> kwInDsx,
                         Deserialiser<Readings> kwOutDsx,
                         Deserialiser<Boolean> cacheableDsx,
                         Deserialiser<EnergyProfileStat> statDsx) {
        this.kwInDsx = kwInDsx;
        this.kwOutDsx = kwOutDsx;
        this.cacheableDsx = cacheableDsx;
        this.statDsx = statDsx;
    }

    public Deserialiser<Readings> kwInDsx() {
        return kwInDsx;
    }

    public Deserialiser<Readings> kwOutDsx() {
        return kwOutDsx;
    }

    public Deserialiser<Boolean> cacheableDsx() {
        return cacheableDsx;
    }

    @SuppressWarnings("WeakerAccess")
    public Deserialiser<EnergyProfileStat> statDsx() {
        return statDsx;
    }

}
