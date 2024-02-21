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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class EnergyProfileStatTest {

    private final String id = "id";
    private final LocalDate date = LocalDate.now(ZoneId.systemDefault());
    private final Readings kwIn = Readings.of(Channel.of(1, 3, 2));
    private final Readings kwOut = Readings.of(Channel.of(5, 4, 0));
    private final EnergyProfile profile = EnergyProfile.of(id, date, kwIn, kwOut);

    @Test
    public void publicConstructor() {
        EnergyProfileStat stat = new EnergyProfileStat(1, 2, 3);
        assertThat(stat.kwIn(), is(1.0));
        assertThat(stat.kwOut(), is(2.0));
        assertThat(stat.kwNet(), is(3.0));
    }

    @Test
    public void ofMax() {
        EnergyProfileStat stat = EnergyProfileStat.ofMax(profile);
        assertThat(stat.kwIn(), is(kwIn.max()));
        assertThat(stat.kwOut(), is(kwOut.max()));
        assertThat(stat.kwNet(), is(profile.kwNet().max()));
    }

    @Test
    public void ofMin() {
        EnergyProfileStat stat = EnergyProfileStat.ofMin(profile);
        assertThat(stat.kwIn(), is(kwIn.min()));
        assertThat(stat.kwOut(), is(kwOut.min()));
        assertThat(stat.kwNet(), is(profile.kwNet().min()));
    }

    @Test
    public void ofAvg() {
        EnergyProfileStat stat = EnergyProfileStat.ofAvg(profile);
        assertThat(stat.kwIn(), is(kwIn.stream().average().orElseThrow(AssertionError::new)));
        assertThat(stat.kwOut(), is(kwOut.stream().average().orElseThrow(AssertionError::new)));
        assertThat(stat.kwNet(), is(profile.kwNet().stream().average().orElseThrow(AssertionError::new)));
    }

    @Test
    public void zeroLengthProfile() {
        expect(() -> EnergyProfileStat.ofAvg(EnergyProfile.of(id, date, null, null)))
            .toThrow(IllegalArgumentException.class);
    }

}
