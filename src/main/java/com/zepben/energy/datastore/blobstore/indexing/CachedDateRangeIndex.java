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
import java.util.*;
import java.util.function.Consumer;

@EverythingIsNonnullByDefault
public class CachedDateRangeIndex implements DateRangeIndex {

    private final DateRangeIndex index;
    private final Map<String, IdDateRange> cache = new HashMap<>();

    public CachedDateRangeIndex(DateRangeIndex backingIndex) {
        this.index = backingIndex;
    }

    @Nullable
    @Override
    public IdDateRange get(String id) {
        IdDateRange range = cache.get(id);
        if (range != null)
            return range;

        range = index.get(id);
        cache.put(id, range);
        return range;
    }

    @Override
    public void forEach(Collection<String> ids, Consumer<IdDateRange> handler) {
        Set<String> lookupIds = new HashSet<>(ids);
        ids.forEach(id -> {
            IdDateRange range = cache.get(id);
            if (range != null) {
                lookupIds.remove(range.id());
                handler.accept(range);
            }
        });

        if (!lookupIds.isEmpty()) {
            index.forEach(lookupIds, dateRange -> {
                cache.put(dateRange.id(), dateRange);
                handler.accept(dateRange);
            });
        }
    }

    @Override
    public void forAll(Consumer<IdDateRange> handler) {
        index.forAll(dateRange -> {
            cache.put(dateRange.id(), dateRange);
            handler.accept(dateRange);
        });
    }

    @Override
    public boolean save(String id, LocalDate from, LocalDate to) {
        IdDateRange dateRange = cache.get(id);
        IdDateRange newDateRange = new IdDateRange(id, from, to);
        if (Objects.equals(dateRange, newDateRange))
            return true;

        cache.put(id, newDateRange);
        return index.save(id, from, to);
    }

    @Override
    public boolean commit() {
        if (!index.commit()) {
            // We don't track what's changed, so we need to clear the whole cache
            cache.clear();
            return false;
        }

        return true;
    }

    @Override
    public boolean rollback() {
        // We don't track what's changed, so we need to clear the whole cache
        cache.clear();
        return index.rollback();
    }

}
