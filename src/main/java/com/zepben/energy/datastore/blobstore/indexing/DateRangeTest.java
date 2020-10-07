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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@EverythingIsNonnullByDefault
public class DateRangeTest {

    private final DateRangeIndex index;

    public DateRangeTest(DateRangeIndex index) {
        this.index = index;
    }

    public boolean idHasDate(String id, LocalDate date) {
        IdDateRange range = index.get(id);
        return range != null && range.isInRange(date);
    }

    public Collection<String> filterIdsWithDate(Collection<String> ids, LocalDate date) {
        List<String> filteredIds = new ArrayList<>();
        index.forEach(ids, range -> {
            if (range.isInRange(date))
                filteredIds.add(range.id());
        });
        return filteredIds;
    }

}
