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
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toMap;

@EverythingIsNonnullByDefault
public class MockDateRangeIndex implements DateRangeIndex {

    private Map<String, IdDateRange> ranges;

    public MockDateRangeIndex(List<IdDateRange> ranges) {
        this.ranges = ranges.stream().collect(toMap(IdDateRange::id, r -> r));
    }

    @Nullable
    @Override
    public IdDateRange get(String id) {
        return ranges.get(id);
    }

    @Override
    public void forEach(Collection<String> ids, Consumer<IdDateRange> handler) {
        ids.forEach(id -> {
            if (ranges.containsKey(id))
                handler.accept(ranges.get(id));
        });
    }

    @Override
    public void forAll(Consumer<IdDateRange> handler) {
        ranges.values().forEach(handler);
    }

    @Override
    public boolean save(String id, LocalDate from, LocalDate to) {
        ranges.put(id, new IdDateRange(id, from, to));
        return true;
    }

    @Override
    public boolean commit() {
        return true;
    }

    @Override
    public boolean rollback() {
        return false;
    }

}
