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
import java.time.ZoneId;

import static com.zepben.testutils.exception.ExpectException.expect;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsEqual.equalTo;

public class EnergyProfileTest {

    private EnergyProfile newProfile(Readings in, Readings out) {
        return EnergyProfile.of("mock", LocalDate.now(ZoneId.systemDefault()), in, out);
    }

    private Readings newReadings(double... values) {
        return Readings.of(Channel.of(values));
    }

    @Test
    public void createNonCacheableProfile() {
        Readings r1 = Readings.of(Channel.of(1));
        Readings r2 = Readings.of(Channel.of(2));
        EnergyProfile profile = EnergyProfile.of("id", LocalDate.now(ZoneId.systemDefault()), r1, r2);
        assertThat(profile.id(), is("id"));
        assertThat(profile.date(), is(LocalDate.now(ZoneId.systemDefault())));
        assertThat(profile.kwIn(), is(r1));
        assertThat(profile.kwOut(), is(r2));
        assertThat(profile.cacheable(), is(false));
    }

    @Test
    public void createCacheableProfile() {
        Readings r1 = Readings.of(Channel.of(1));
        Readings r2 = Readings.of(Channel.of(2));
        EnergyProfile profile = EnergyProfile.ofCacheable("id", LocalDate.now(ZoneId.systemDefault()), r1, r2);

        assertThat(profile.id(), is("id"));
        assertThat(profile.date(), is(LocalDate.now(ZoneId.systemDefault())));
        assertThat(profile.kwIn(), is(r1));
        assertThat(profile.kwOut(), is(r2));
        assertThat(profile.cacheable(), is(true));
    }

    @Test
    public void usesEmptyReadingsWhenNull() {
        EnergyProfile profile = EnergyProfile.of("id", LocalDate.now(ZoneId.systemDefault()), null, null, true);
        assertThat(profile.kwIn(), sameInstance(Readings.EMPTY_READINGS));
        assertThat(profile.kwOut(), sameInstance(Readings.EMPTY_READINGS));
    }

    @Test
    public void nullKwInUsesMissingCache() {
        Readings kwOut = Readings.of(Channel.of(1, 4));
        EnergyProfile profile = EnergyProfile.of("id", LocalDate.now(ZoneId.systemDefault()), null, kwOut);
        assertThat(profile.kwIn(), sameInstance(ZeroedReadingsCache.ofMissing(kwOut.length())));
    }

    @Test
    public void nullKwOutUsesMissingCache() {
        Readings kwIn = Readings.of(Channel.of(1, 4));
        EnergyProfile profile = EnergyProfile.of("id", LocalDate.now(ZoneId.systemDefault()), kwIn, null);
        assertThat(profile.kwOut(), sameInstance(ZeroedReadingsCache.ofMissing(kwIn.length())));
    }

    @Test
    public void kwNet() {
        Readings kwIn = Readings.of(Channel.of(2, 2));
        Readings kwOut = Readings.of(Channel.of(1, 4));
        EnergyProfile profile = EnergyProfile.of("id", LocalDate.now(ZoneId.systemDefault()), kwIn, kwOut);
        assertThat(profile.kwNet().length(), is(2));
        assertThat(profile.kwNet().get(0), is(1.0));
        assertThat(profile.kwNet().get(1), is(-2.0));
    }

    @Test
    public void KwNetUsesCachedValueWithZeroLenReadings() {
        EnergyProfile profile = EnergyProfile.of("id", LocalDate.now(ZoneId.systemDefault()), Readings.EMPTY_READINGS, Readings.EMPTY_READINGS);
        assertThat(profile.kwNet(), sameInstance(Readings.EMPTY_READINGS));
    }

    @Test
    public void notEqualReadingsLength() {
        Readings kwIn = Readings.of(Channel.of(1, 1));
        Readings kwOut = Readings.of(Channel.of(1));

        expect(() -> EnergyProfile.of("id", LocalDate.now(ZoneId.systemDefault()), kwIn, kwOut))
            .toThrow(IllegalArgumentException.class);
    }

    @Test
    public void kwNetDifferentLengthReadingsThrows() {
        expect(() -> {
            EnergyProfile profile = newProfile(newReadings(), newReadings(5));
            assertThat(profile.kwNet(), equalTo(newReadings(5)));
        }).toThrow(IllegalArgumentException.class);
    }

    @Test
    public void kwNetZeroLengthReadings() {
        expect(() -> {
            EnergyProfile profile = newProfile(newReadings(), newReadings());
            assertThat(profile.kwNet().length(), equalTo(0));
            profile.kwNet().get(0);
        }).toThrow(IndexOutOfBoundsException.class);
    }

    @Test
    public void equals() {
        Readings kwIn = newReadings(1.1);
        Readings kwOut = newReadings(2.2);
        EnergyProfile left = newProfile(kwIn, kwOut);
        EnergyProfile right = newProfile(kwIn, kwOut);
        assertThat(left, equalTo(right));
    }

    @Test
    public void notEquals() {
        Readings kwIn = newReadings(1.1);
        Readings kwOut = newReadings(2.2);
        EnergyProfile left = newProfile(kwIn, kwOut);
        EnergyProfile right = newProfile(kwIn, newReadings(3.3));
        assertThat(left, not(equalTo(right)));
    }

    @Test
    public void hashEqual() {
        Readings kwIn = newReadings(1.1);
        Readings kwOut = newReadings(2.2);
        EnergyProfile left = newProfile(kwIn, kwOut);
        EnergyProfile right = newProfile(kwIn, kwOut);
        assertThat(left.hashCode(), equalTo(right.hashCode()));
    }

    @Test
    public void hashNotEqual() {
        Readings kwIn = newReadings(1.1);
        Readings kwOut = newReadings(2.2);
        EnergyProfile left = newProfile(kwIn, kwOut);
        EnergyProfile right = newProfile(kwIn, newReadings(3.3));
        assertThat(left.hashCode(), not(equalTo(right.hashCode())));
    }

    @Test
    public void toStringContainsMembers() {
        Readings kwIn = newReadings(1.1);
        Readings kwOut = newReadings(2.2);
        EnergyProfile profile = newProfile(kwIn, kwOut);
        String toString = profile.toString();
        assertThat(toString, containsString("EnergyProfile"));
        assertThat(toString, containsString("id=" + profile.id()));
        assertThat(toString, containsString("date=" + profile.date()));
        assertThat(toString, containsString("kwIn=" + profile.kwIn()));
        assertThat(toString, containsString("kwOut=" + profile.kwOut()));
        assertThat(toString, containsString("cacheable=" + profile.cacheable()));
    }

}
