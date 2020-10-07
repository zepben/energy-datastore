/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.datastore.blobstore;

import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.energy.model.EnergyProfile;
import com.zepben.energy.model.Readings;

import javax.annotation.Nullable;
import java.time.LocalDate;

@EverythingIsNonnullByDefault
public interface EnergyProfileFactory {

    EnergyProfile create(String id,
                         LocalDate date,
                         @Nullable Readings kwIn,
                         @Nullable Readings kwOut,
                         boolean cacheable);

}
