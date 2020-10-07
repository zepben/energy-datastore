/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.datastore;

import com.zepben.annotations.EverythingIsNonnullByDefault;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.Collection;

@EverythingIsNonnullByDefault
public interface PartialProfileReader<T> {

    @Nullable
    T get(String id, LocalDate date, ErrorHandler onError);

    void forEach(Collection<String> ids,
                 LocalDate date,
                 ItemHandler<T> onRead,
                 ErrorHandler onError);

    void forAll(LocalDate date,
                ItemHandler<T> onRead,
                ErrorHandler onError);

}
