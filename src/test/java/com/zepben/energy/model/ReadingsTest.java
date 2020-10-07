/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.model;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.zepben.testutils.exception.ExpectException.expect;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class ReadingsTest {

//    @EverythingIsNonnullByDefault
//    class MockReadings extends Readings {
//        private Channel[] channels;
//
//        public MockReadings(Channel... channels) {
//            this.channels = channels;
//        }
//
//        @Override
//        public int numChannels() {
//            return channels.length;
//        }
//
//        @Override
//        public Channel channel(int i) {
//            return channels[i - 1];
//        }
//
//        @Override
//        public int length() {
//            return channels[0].length();
//        }
//    }

    @Test
    public void singleChannelFactory() {
        assertThat(Readings.of(Channel.of(1.)), instanceOf(OneChannelReadings.class));
    }

    @Test
    public void emptyChannelReturnsEmptyReadings() {
        assertThat(Readings.of(Channel.EMPTY_CHANNEL), sameInstance(Readings.EMPTY_READINGS));
    }

    @Test
    public void multiChannelFactory() {
        assertThat(Readings.of(Channel.of(1.), Channel.of(1.)), instanceOf(MultiChannelReadings.class));
    }

    @Test
    public void multiChannelFactoryCreatesSingleChannelReadingsWhenOneChannel() {
        assertThat(Readings.of(new Channel[]{Channel.of(1.)}), instanceOf(OneChannelReadings.class));
    }

    @Test
    public void zeroLengthReadingsThrows() {
        expect(Readings::of).toThrow(IllegalArgumentException.class);
    }

    @Test
    public void channelStream() {
        Channel[] channels = {Channel.of(1.), Channel.of(1.)};
        Readings readings = Readings.of(channels);
        assertThat(readings.channelStream().toArray(), equalTo(channels));
    }

    @Test
    public void getAggregatesChannels() {
        Channel[] channels = {Channel.of(1, 2), Channel.of(4, 5)};
        Readings readings = Readings.of(channels);
        assertThat(readings.get(0), is(5.));
        assertThat(readings.get(1), is(7.));
    }

    @Test
    public void equalEquals() {
        Readings r1 = Readings.of(Channel.of(1.), Channel.of(5.));
        Readings r2 = Readings.of(Channel.of(1.), Channel.of(5.));
        assertThat(r1, equalTo(r2));
    }

    @Test
    public void notEquals() {
        Readings r1 = Readings.of(Channel.of(1.), Channel.of(5.));
        Readings r2 = Readings.of(Channel.of(2.), Channel.of(5.));
        assertThat(r1, not(equalTo(r2)));
    }

    @Test
    public void equalHashCode() {
        Readings r1 = Readings.of(Channel.of(1.), Channel.of(5.));
        Readings r2 = Readings.of(Channel.of(1.), Channel.of(5.));
        assertThat(r1.hashCode(), Matchers.equalTo(r2.hashCode()));
    }

    @Test
    public void notEqualHashCode() {
        Readings r1 = Readings.of(Channel.of(1.), Channel.of(5.));
        Readings r2 = Readings.of(Channel.of(2.), Channel.of(5.));
        assertThat(r1.hashCode(), not(Matchers.equalTo(r2.hashCode())));
    }

    @Test
    public void toStringContainsMembers() {
        Channel[] channels = {Channel.of(1., 2.), Channel.of(3., 4.)};
        Readings readings = Readings.of(channels);
        String toString = readings.toString();
        assertThat(toString, containsString("MultiChannelReadings"));
        assertThat(toString, containsString("channels=" + Arrays.toString(readings.channelStream().toArray())));
    }

}
