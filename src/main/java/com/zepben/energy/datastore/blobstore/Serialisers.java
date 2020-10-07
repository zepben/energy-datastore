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
public class Serialisers {

    private final Serialiser<Readings> kwInSx;
    private final Serialiser<Readings> kwOutSx;
    private final Serialiser<Boolean> cacheableSx;
    private final Serialiser<EnergyProfileStat> statSx;

    public Serialisers(Serialiser<Readings> kwInSx,
                       Serialiser<Readings> kwOutSx,
                       Serialiser<Boolean> cacheableSx,
                       Serialiser<EnergyProfileStat> statSx) {
        this.kwInSx = kwInSx;
        this.kwOutSx = kwOutSx;
        this.cacheableSx = cacheableSx;
        this.statSx = statSx;
    }

    public Serialiser<Readings> kwInSx() {
        return kwInSx;
    }

    public Serialiser<Readings> kwOutSx() {
        return kwOutSx;
    }

    public Serialiser<Boolean> cacheableSx() {
        return cacheableSx;
    }

    @SuppressWarnings("WeakerAccess")
    public Serialiser<EnergyProfileStat> statSx() {
        return statSx;
    }

}
