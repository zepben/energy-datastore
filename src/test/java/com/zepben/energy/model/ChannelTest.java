/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.model;


import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ChannelTest {

    @Test
    public void ofDoubles() {
        double d = 1;
        assertThat(Channel.of(d), instanceOf(DoubleChannel.class));
    }

    @Test
    public void ofFloats() {
        float f = 1;
        assertThat(Channel.of(f), instanceOf(FloatChannel.class));
    }

    @Test
    public void ofFloatsFromDoubles() {
        double d = 1;
        assertThat(Channel.ofFloats(d), instanceOf(FloatChannel.class));
    }

    @Test
    public void equalEquals() {
        Channel c1 = Channel.of(1.);
        Channel c2 = Channel.of(1.);
        assertThat(c1, equalTo(c2));
    }

    @Test
    public void notEquals() {
        Channel c1 = Channel.of(1.);
        Channel c2 = Channel.of(2.);
        assertThat(c1, not(equalTo(c2)));
    }

    @Test
    public void equalHashCode() {
        Channel c1 = Channel.of(1.);
        Channel c2 = Channel.of(1.);
        assertThat(c1.hashCode(), equalTo(c2.hashCode()));
    }

    @Test
    public void notEqualHashCode() {
        Channel c1 = Channel.of(1.);
        Channel c2 = Channel.of(2.);
        assertThat(c1.hashCode(), not(equalTo(c2.hashCode())));
    }

    @Test
    public void toStringContainsMembers() {
        Channel channel = Channel.of(1.);
        String toString = channel.toString();
        assertThat(toString, containsString("DoubleChannel"));
        assertThat(toString, containsString("values=" + Arrays.toString(channel.stream().toArray())));
    }

}
