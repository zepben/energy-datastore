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
public class ZeroedChannel extends Channel {

    private final int length;

    ZeroedChannel(int length) {
        this.length = length;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public double get(int i) {
        if (i < 0 || i >= length)
            throw new IllegalArgumentException("index must be in range 0 <= i < length()");

        return 0;
    }

}
