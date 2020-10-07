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
class OneChannelReadings extends Readings {

    private final Channel channel;

    OneChannelReadings(Channel channel) {
        this.channel = channel;
    }

    @Override
    public int numChannels() {
        return 1;
    }

    @Override
    public Channel channel(int i) {
        return channel;
    }

    @Override
    public int length() {
        return channel.length();
    }

}
