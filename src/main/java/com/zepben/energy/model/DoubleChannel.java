/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.model;

import com.zepben.annotations.EverythingIsNonnullByDefault;

import java.util.Arrays;

/**
 * Immutable Readings backed by a double array
 */
@EverythingIsNonnullByDefault
class DoubleChannel extends Channel {

    private final double[] values;

    DoubleChannel(double... values) {
        this.values = Arrays.copyOf(values, values.length);
    }

    @Override
    final public int length() {
        return values.length;
    }

    @Override
    final public double get(int i) {
        return values[i];
    }

}
