/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.model;


import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ZeroedReadingsCacheTest {

    @Test
    public void ofStaticInstances() throws Exception {
        assertThat(ZeroedReadingsCache.of(1, 0), sameInstance(ZeroedReadingsCache.ZEROED_1_CHANNEL_0));
        assertThat(ZeroedReadingsCache.of(1, 48), sameInstance(ZeroedReadingsCache.ZEROED_1_CHANNEL_48));
        assertThat(ZeroedReadingsCache.of(1, 96), sameInstance(ZeroedReadingsCache.ZEROED_1_CHANNEL_96));
    }

    @Test
    public void of() throws Exception {
        Readings readings = ZeroedReadingsCache.of(1, 2);
        assertThat(readings, instanceOf(ZeroedReadings.class));
        assertThat(readings.numChannels(), is(1));
        assertThat(readings.length(), is(2));
    }

    @Test
    public void ofMissingStaticInstances() throws Exception {
        assertThat(ZeroedReadingsCache.ofMissing(0), sameInstance(ZeroedReadingsCache.MISSING_READINGS_0));
        assertThat(ZeroedReadingsCache.ofMissing(48), sameInstance(ZeroedReadingsCache.MISSING_READINGS_48));
        assertThat(ZeroedReadingsCache.ofMissing(96), sameInstance(ZeroedReadingsCache.MISSING_READINGS_96));
    }

    @Test
    public void ofMissing() throws Exception {
        Readings readings = ZeroedReadingsCache.ofMissing(2);
        assertThat(readings, instanceOf(MissingReadings.class));
        assertThat(readings.numChannels(), is(1));
        assertThat(readings.length(), is(2));
    }

}