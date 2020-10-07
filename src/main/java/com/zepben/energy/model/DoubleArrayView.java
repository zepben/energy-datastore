/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.model;

import com.zepben.annotations.EverythingIsNonnullByDefault;

import javax.annotation.Nullable;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * Interface that defines common methods for things that are backed by an "array" of floating point values.
 */
@EverythingIsNonnullByDefault
public interface DoubleArrayView {

    int length();

    double get(int i);

    default double max() {
        if (length() <= 0)
            throw new IllegalStateException("can't get max of 0 length readings");

        double max = get(0);
        for (int i = 1, len = length(); i < len; ++i) {
            double d = get(i);
            if (d > max)
                max = d;
        }

        return max;
    }

    default double min() {
        if (length() <= 0)
            throw new IllegalStateException("can't get min of 0 length readings");

        double min = get(0);
        for (int i = 1, len = length(); i < len; ++i) {
            double d = get(i);
            if (d < min)
                min = d;
        }

        return min;
    }

    default DoubleStream stream() {
        return IntStream.range(0, length()).mapToDouble(this::get);
    }

    default boolean valuesEqual(@Nullable DoubleArrayView other) {
        if (other == this) return true;
        if (other == null) return false;

        int thisLen = length();
        int thatLen = other.length();
        if (thisLen != thatLen)
            return false;

        for (int i = 0; i < thisLen; ++i) {
            if (Double.compare(get(i), other.get(i)) != 0)
                return false;
        }

        return true;
    }

}
