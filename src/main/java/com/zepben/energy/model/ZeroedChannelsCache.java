/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.model;

import com.zepben.annotations.EverythingIsNonnullByDefault;

import java.util.HashMap;
import java.util.Map;

@EverythingIsNonnullByDefault
final public class ZeroedChannelsCache {

    private static final ZeroedChannel CHANNEL_LEN_0 = new ZeroedChannel(0);
    private static final ZeroedChannel CHANNEL_LEN_48 = new ZeroedChannel(48);
    private static final ZeroedChannel CHANNEL_LEN_96 = new ZeroedChannel(96);

    private static final Map<Integer, ZeroedChannel> cache = new HashMap<>();

    @SuppressWarnings("Duplicates")
    public static ZeroedChannel of(int length) {
        switch (length) {
            case 0:
                return CHANNEL_LEN_0;
            case 48:
                return CHANNEL_LEN_48;
            case 96:
                return CHANNEL_LEN_96;
        }

        return cache.computeIfAbsent(length, ZeroedChannel::new);
    }

}
