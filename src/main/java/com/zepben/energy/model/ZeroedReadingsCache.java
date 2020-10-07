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
final public class ZeroedReadingsCache {

    static final ZeroedReadings ZEROED_1_CHANNEL_0 = new ZeroedReadings(1, 0);
    static final ZeroedReadings ZEROED_1_CHANNEL_48 = new ZeroedReadings(1, 48);
    static final ZeroedReadings ZEROED_1_CHANNEL_96 = new ZeroedReadings(1, 96);

    static final MissingReadings MISSING_READINGS_0 = new MissingReadings(0);
    static final MissingReadings MISSING_READINGS_48 = new MissingReadings(48);
    static final MissingReadings MISSING_READINGS_96 = new MissingReadings(96);

    private static final Map<Integer, Map<Integer, ZeroedReadings>> zeroedCache = new HashMap<>();
    private static final Map<Integer, MissingReadings> missingCache = new HashMap<>();

    @SuppressWarnings("Duplicates")
    public static ZeroedReadings of(int numChannels, int length) {
        if (numChannels == 1) {
            switch (length) {
                case 0:
                    return ZEROED_1_CHANNEL_0;
                case 48:
                    return ZEROED_1_CHANNEL_48;
                case 96:
                    return ZEROED_1_CHANNEL_96;
            }
        }

        return zeroedCache.computeIfAbsent(numChannels, nChannels -> new HashMap<>())
            .computeIfAbsent(length, len -> new ZeroedReadings(numChannels, len));
    }

    @SuppressWarnings({"Duplicates", "WeakerAccess"})
    public static MissingReadings ofMissing(int length) {
        switch (length) {
            case 0:
                return MISSING_READINGS_0;
            case 48:
                return MISSING_READINGS_48;
            case 96:
                return MISSING_READINGS_96;
        }

        return missingCache.computeIfAbsent(length, MissingReadings::new);
    }

}
