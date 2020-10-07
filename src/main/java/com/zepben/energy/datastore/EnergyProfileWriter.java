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
import com.zepben.energy.model.Readings;

import java.time.LocalDate;

@EverythingIsNonnullByDefault
public interface EnergyProfileWriter {

    default boolean write(EnergyProfile profile, ErrorHandler onError) {
        return write(profile, false, onError);
    }

    boolean write(EnergyProfile profile, boolean writeStats, ErrorHandler onError);

    boolean commit(ErrorHandler onError);

    boolean rollback(ErrorHandler onError);

    @SuppressWarnings("SameParameterValue")
    boolean writeKwIn(String id,
                      LocalDate date,
                      Readings readings,
                      ErrorHandler onError);

    @SuppressWarnings("SameParameterValue")
    boolean writeKwOut(String id,
                       LocalDate date,
                       Readings readings,
                       ErrorHandler onError);

    @SuppressWarnings("SameParameterValue")
    boolean writeCacheable(String id,
                           LocalDate date,
                           boolean cacheable,
                           ErrorHandler onError);

}
