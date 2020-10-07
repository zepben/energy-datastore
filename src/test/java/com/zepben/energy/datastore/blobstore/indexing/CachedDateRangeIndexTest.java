/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.datastore.blobstore.indexing;

import com.zepben.energy.model.IdDateRange;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

public class CachedDateRangeIndexTest {

    private DateRangeIndex index = mock(DateRangeIndex.class);
    private CachedDateRangeIndex cachedIndex = new CachedDateRangeIndex(index);

    private String id = "id";
    private LocalDate from = LocalDate.now();
    private LocalDate to = from.plusDays(1);
    private IdDateRange expectedRange = new IdDateRange(id, from, to);

    @Test
    public void get() throws Exception {
        doReturn(expectedRange).when(index).get(id);

        IdDateRange range = cachedIndex.get(id);
        assertThat(range, equalTo(expectedRange));

        range = cachedIndex.get(id);
        assertThat(range, equalTo(expectedRange));

        verify(index, times(1)).get(id);
    }

    @Test
    public void forEach() throws Exception {
        List<String> ids = Collections.singletonList(id);
        doAnswer(inv -> {
            Consumer<IdDateRange> handler = inv.getArgument(1);
            handler.accept(expectedRange);
            return null;
        }).when(index).forEach(eq(Collections.singleton(id)), any());

        List<IdDateRange> ranges = new ArrayList<>();
        cachedIndex.forEach(ids, ranges::add);
        assertThat(ranges, equalTo(Collections.singletonList(expectedRange)));

        ranges.clear();
        cachedIndex.forEach(ids, ranges::add);
        assertThat(ranges, equalTo(Collections.singletonList(expectedRange)));

        verify(index, times(1)).forEach(any(), any());
    }

    @Test
    public void forAll() throws Exception {
        doAnswer(inv -> {
            Consumer<IdDateRange> handler = inv.getArgument(0);
            handler.accept(expectedRange);
            return null;
        }).when(index).forAll(any());

        List<IdDateRange> ranges = new ArrayList<>();
        cachedIndex.forAll(ranges::add);
        assertThat(ranges, equalTo(Collections.singletonList(expectedRange)));

        IdDateRange range = cachedIndex.get(id);
        assertThat(range, equalTo(expectedRange));

        verify(index, times(1)).forAll(any());
        verify(index, never()).get(id);
    }

    @Test
    public void cachesOnsave() throws Exception {
        cachedIndex.save(expectedRange.id(), expectedRange.from(), expectedRange.to());
        verify(index).save(expectedRange.id(), expectedRange.from(), expectedRange.to());

        IdDateRange range = cachedIndex.get(id);
        assertThat(range, equalTo(expectedRange));

        verify(index, never()).get(id);
    }

    @Test
    public void doesNotResaveWhenCachedValueEqual() throws Exception {
        cachedIndex.save(expectedRange.id(), expectedRange.from(), expectedRange.to());
        cachedIndex.save(expectedRange.id(), expectedRange.from(), expectedRange.to());
        verify(index, times(1)).save(expectedRange.id(), expectedRange.from(), expectedRange.to());
    }

    @Test
    public void commits() throws Exception {
        doReturn(true).when(index).commit();
        cachedIndex.commit();
        verify(index).commit();
    }

    @Test
    public void clearsCacheOnCommitError() throws Exception {
        doReturn(false).when(index).commit();

        cachedIndex.save(expectedRange.id(), expectedRange.from(), expectedRange.to());
        cachedIndex.commit();
        verify(index).commit();

        cachedIndex.get(id);
        verify(index, times(1)).get(id);
    }

    @Test
    public void rollsback() throws Exception {
        doReturn(true).when(index).rollback();
        cachedIndex.rollback();
        verify(index).rollback();

        cachedIndex.get(id);
        verify(index, times(1)).get(id);
    }

}