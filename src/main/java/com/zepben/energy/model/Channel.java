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
import java.util.Arrays;

@EverythingIsNonnullByDefault
public abstract class Channel implements DoubleArrayView {

    @SuppressWarnings("WeakerAccess")
    public static final Channel EMPTY_CHANNEL = new Channel() {
        @Override
        public int length() {
            return 0;
        }

        @Override
        public double get(int i) {
            throw new IndexOutOfBoundsException("can't get from channel with no values");
        }
    };

    public static Channel of(double... values) {
        return new DoubleChannel(values);
    }

    public static Channel of(float... values) {
        return new FloatChannel(values);
    }

    public static Channel ofFloats(double... values) {
        return new FloatChannel(values);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof Channel)) return false;
        Channel that = (Channel) o;
        return valuesEqual(that);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(stream().toArray());
    }

    @Override
    public String toString() {
        String name = getClass().getSimpleName();
        if (name.equals(""))
            name = getClass().getName();

        return name + "{" +
            "values=" + Arrays.toString(stream().toArray()) +
            '}';
    }

}
