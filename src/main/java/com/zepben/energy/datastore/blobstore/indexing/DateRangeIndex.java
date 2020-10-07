/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.datastore.blobstore.indexing;

import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.energy.model.IdDateRange;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.Collection;
import java.util.function.Consumer;

@EverythingIsNonnullByDefault
public interface DateRangeIndex {

    @Nullable
    IdDateRange get(String id);

    void forEach(Collection<String> ids, Consumer<IdDateRange> handler);

    void forAll(Consumer<IdDateRange> handler);

    boolean save(String id, LocalDate from, LocalDate to);

    default boolean extendRange(String id, LocalDate date) {
        IdDateRange range = get(id);
        if (range == null)
            return save(id, date, date);

        if (date.isBefore(range.from()))
            return saveFrom(id, date);
        else if (date.isAfter(range.to()))
            return saveTo(id, date);

        // Nothing to update
        return true;
    }

    default boolean saveFrom(String id, LocalDate from) {
        IdDateRange range = get(id);
        return range != null &&
            !range.from().equals(from) &&
            isValidRange(from, range.to()) &&
            save(id, from, range.to());
    }

    default boolean saveTo(String id, LocalDate to) {
        IdDateRange range = get(id);
        return range != null &&
            !range.to().equals(to) &&
            isValidRange(range.from(), to) &&
            save(id, range.from(), to);
    }

    boolean commit();

    boolean rollback();

    default boolean isValidRange(LocalDate from, LocalDate to) {
        return !from.isAfter(to);
    }

}
