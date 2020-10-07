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
class MultiChannelReadings extends Readings {

    private final Channel[] channels;

    MultiChannelReadings(Channel[] channels) {
        if (channels.length == 0)
            throw new IllegalArgumentException("channels must have a length greater than 0");

        if (channels.length == 1)
            throw new IllegalArgumentException("INTERNAL ERROR: Use OneChannelReadings for one channel");

        this.channels = new Channel[channels.length];
        Integer valuesLen = channels[0].length();
        for (int i = 0; i < channels.length; ++i) {
            if (channels[i].length() != valuesLen)
                throw new IllegalArgumentException("all channels must have the same number of values");

            this.channels[i] = channels[i];
        }
    }

    @Override
    public int length() {
        return channels[0].length();
    }

    @Override
    public int numChannels() {
        return channels.length;
    }

    @Override
    public Channel channel(int i) {
        return channels[i - 1];
    }

}
