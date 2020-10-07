/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.model;

import com.zepben.annotations.EverythingIsNonnullByDefault;

@EverythingIsNonnullByDefault
public class ZeroedReadings extends Readings {

    private final int numChannels;
    private final Channel channel;

    ZeroedReadings(int numChannels, int length) {
        this.numChannels = numChannels;
        channel = ZeroedChannelsCache.of(length);
    }

    @Override
    public int numChannels() {
        return numChannels;
    }

    @Override
    public Channel channel(int i) {
        if (i < 1 || i > numChannels)
            throw new IllegalArgumentException("channel number must be in range 1 <= i <= numChannels()");

        return channel;
    }

    @Override
    public int length() {
        return channel.length();
    }

}
