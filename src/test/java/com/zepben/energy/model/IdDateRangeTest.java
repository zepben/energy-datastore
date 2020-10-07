/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.model;


import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class IdDateRangeTest {

    private String id = "id";
    private LocalDate from = LocalDate.now();
    private LocalDate to = from.plusDays(7);
    private IdDateRange range = new IdDateRange(id, from, to);

    @Test
    public void id() {
        assertThat(range.id(), is(id));
    }

    @Test
    public void from() {
        assertThat(range.from(), is(from));
    }

    @Test
    public void to() {
        assertThat(range.to(), is(to));
    }

    @Test
    public void isInRange() {
        assertThat(range.isInRange(from), is(true));
        assertThat(range.isInRange(to), is(true));
        assertThat(range.isInRange(from.plusDays(1)), is(true));
        assertThat(range.isInRange(from.minusDays(1)), is(false));
        assertThat(range.isInRange(to.plusDays(1)), is(false));
    }

    @Test
    public void equals() {
        IdDateRange range1 = new IdDateRange(id, from, to);
        IdDateRange range2 = new IdDateRange(id, from, to);
        assertThat(range1, equalTo(range2));
    }

    @Test
    public void notEquals() {
        IdDateRange range1 = new IdDateRange(id, from, to);
        IdDateRange range2 = new IdDateRange(id, from, to.plusDays(1));
        assertThat(range1, not(equalTo(range2)));
    }

    @Test
    public void hashCodeEquals() {
        IdDateRange range1 = new IdDateRange(id, from, to);
        IdDateRange range2 = new IdDateRange(id, from, to);
        assertThat(range1.hashCode(), equalTo(range2.hashCode()));
    }

    @Test
    public void hasCodeNotEquals() {
        IdDateRange range1 = new IdDateRange(id, from, to);
        IdDateRange range2 = new IdDateRange(id, from, to.plusDays(1));
        assertThat(range1.hashCode(), not(equalTo(range2.hashCode())));
    }

}
