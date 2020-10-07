/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.model;

import com.zepben.annotations.EverythingIsNonnullByDefault;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.Objects;

@EverythingIsNonnullByDefault
public abstract class EnergyProfile {

    private final String id;
    private final LocalDate date;
    private final Readings kwIn;
    private final Readings kwOut;

    public static EnergyProfile of(String id,
                                   LocalDate date,
                                   @Nullable Readings kwIn,
                                   @Nullable Readings kwOut) {
        return of(id, date, kwIn, kwOut, false);
    }

    public static EnergyProfile ofCacheable(String id,
                                            LocalDate date,
                                            @Nullable Readings kwIn,
                                            @Nullable Readings kwOut) {
        return of(id, date, kwIn, kwOut, true);
    }

    public static EnergyProfile of(String id,
                                   LocalDate date,
                                   @Nullable Readings kwIn,
                                   @Nullable Readings kwOut,
                                   boolean isCacheable) {
        if (kwIn == null && kwOut == null) {
            kwIn = Readings.EMPTY_READINGS;
            kwOut = Readings.EMPTY_READINGS;
        } else if (kwIn == null) {
            kwIn = ZeroedReadingsCache.ofMissing(kwOut.length());
        } else if (kwOut == null) {
            kwOut = ZeroedReadingsCache.ofMissing(kwIn.length());
        }

        // NOTE: This makes anonymous instances using constant values for the cacheable flag. Avoiding using a class
        //       member or capturing the isCacheable arg in the anonymous class means that each instance takes up slightly
        //       less memory. We are currently storing tens of millions of these in memory and that starts to add up.
        if (isCacheable) {
            return new EnergyProfile(id, date, kwIn, kwOut) {
                @Override
                final public boolean cacheable() {
                    return true;
                }
            };
        } else {
            return new EnergyProfile(id, date, kwIn, kwOut) {
                @Override
                final public boolean cacheable() {
                    return false;
                }
            };
        }
    }

    protected EnergyProfile(String id, LocalDate date, Readings kwIn, Readings kwOut) {
        if (kwIn.length() != kwOut.length())
            throw new IllegalArgumentException("Readings must have the same length");

        this.id = id;
        this.date = date;
        this.kwIn = kwIn;
        this.kwOut = kwOut;
    }

    public String id() {
        return id;
    }

    public LocalDate date() {
        return date;
    }

    public Readings kwIn() {
        return kwIn;
    }

    public Readings kwOut() {
        return kwOut;
    }

    @SuppressWarnings("WeakerAccess")
    public Readings kwNet() {
        if (kwIn().length() == 0 && kwOut().length() == 0)
            return Readings.EMPTY_READINGS;

        return Readings.of(new KwNetChannel(kwIn, kwOut));
    }

    public abstract boolean cacheable();

    @EverythingIsNonnullByDefault
    private static class KwNetChannel extends Channel {

        private final Readings kwIn;
        private final Readings kwOut;

        KwNetChannel(Readings kwIn, Readings kwOut) {
            this.kwIn = kwIn;
            this.kwOut = kwOut;
        }

        @Override
        public int length() {
            return kwIn.length();
        }

        @Override
        public double get(int i) {
            return kwIn.get(i) - kwOut.get(i);
        }

    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnergyProfile profile = (EnergyProfile) o;
        return Objects.equals(id, profile.id) &&
            Objects.equals(date, profile.date) &&
            Objects.equals(kwIn, profile.kwIn) &&
            Objects.equals(kwOut, profile.kwOut);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, date, kwIn, kwOut);
    }

    @Override
    public String toString() {
        String name = getClass().getSimpleName();
        if (name.equals(""))
            name = getClass().getName();

        return name + "{" +
            "id=" + id +
            ", date=" + date +
            ", kwIn=" + kwIn +
            ", kwOut=" + kwOut +
            ", cacheable=" + cacheable() +
            '}';
    }

}
