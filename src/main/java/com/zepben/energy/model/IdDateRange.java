/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.model;

import com.zepben.annotations.EverythingIsNonnullByDefault;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Class that holds a date range represented by from and to dates.
 */
@EverythingIsNonnullByDefault
public final class IdDateRange {

    private final String id;
    private final LocalDate from;
    private final LocalDate to;

    public IdDateRange(String id, LocalDate from, LocalDate to) {
        this.id = id;
        if (from.isBefore(to)) {
            this.from = from;
            this.to = to;
        } else {
            this.from = to;
            this.to = from;
        }
    }

    public String id() {
        return id;
    }

    public LocalDate from() {
        return from;
    }

    public LocalDate to() {
        return to;
    }

    public boolean isInRange(LocalDate date) {
        return !date.isBefore(from) && !date.isAfter(to);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdDateRange idDateRange = (IdDateRange) o;
        return Objects.equals(from, idDateRange.from) &&
            Objects.equals(to, idDateRange.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

}
