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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DoubleArrayViewTest {

    private class MockDoubleArrayView implements DoubleArrayView {

        private final double[] values;

        MockDoubleArrayView(double... values) {
            this.values = values;
        }

        @Override
        public int length() {
            return values.length;
        }

        @Override
        public double get(int i) {
            return values[i];
        }

    }

    private DoubleArrayView array = new MockDoubleArrayView(4., -1, 10, 6);

    @Test
    public void max() {
        assertThat(array.max(), is(10.));
    }

    @Test
    public void zeroLengthMaxThrows() {
        MockDoubleArrayView zeroLenArray = new MockDoubleArrayView();
        expect(zeroLenArray::max).toThrow(IllegalStateException.class);
    }

    @Test
    public void zeroLengthMinThrows() {
        MockDoubleArrayView zeroLenArray = new MockDoubleArrayView();
        expect(zeroLenArray::min).toThrow(IllegalStateException.class);
    }

    @Test
    public void min() {
        assertThat(array.min(), is(-1.));
    }

    @Test
    public void stream() {
        double[] streamArray = array.stream().toArray();
        assertThat(array.valuesEqual(new MockDoubleArrayView(streamArray)), is(true));
    }

    @Test
    public void valuesEqual() {
        MockDoubleArrayView other = new MockDoubleArrayView(array.stream().toArray());
        assertThat(array.valuesEqual(other), is(true));
    }

    @Test
    public void valuesEqualSameInstance() {
        assertThat(array.valuesEqual(array), is(true));
    }

    @Test
    public void valuesNotEqual() {
        MockDoubleArrayView other = new MockDoubleArrayView(array.stream().map(i -> i + 1).toArray());
        assertThat(array.valuesEqual(other), is(false));
    }

    @Test
    public void valuesDifferentLengthNotEqual() {
        MockDoubleArrayView other = new MockDoubleArrayView(1);
        assertThat(array.valuesEqual(other), is(false));
    }

    @Test
    public void valuesEqualHandlesNull() {
        assertThat(array.valuesEqual(null), is(false));
    }

}
