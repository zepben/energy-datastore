/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.model;


import org.junit.jupiter.api.Test;

import static com.zepben.testutils.exception.ExpectException.expect;

public class MultiChannelReadingsTest {

    @Test
    public void zeroLengthChannelsThrows() {
        expect(() -> new MultiChannelReadings(new Channel[]{})).toThrow(IllegalArgumentException.class);
    }

    @Test
    public void singleLengthChannelThrows() {
        expect(() -> new MultiChannelReadings(new Channel[]{Channel.of(1.)})).toThrow(IllegalArgumentException.class);
    }

    @Test
    public void differentLengthChannelValuesThrows() {
        Channel[] channels = {Channel.of(1), Channel.of(1, 2)};
        expect(() -> new MultiChannelReadings(channels)).toThrow(IllegalArgumentException.class);
    }

}
