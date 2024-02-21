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
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A readings class provides access to channel values as well as providing aggregated channel values.
 */
@EverythingIsNonnullByDefault
public abstract class Readings implements DoubleArrayView {

    public static final Readings EMPTY_READINGS = new Readings() {
        @Override
        public int numChannels() {
            return 1;
        }

        @Override
        public Channel channel(int i) {
            return Channel.EMPTY_CHANNEL;
        }

        @Override
        public int length() {
            return Channel.EMPTY_CHANNEL.length();
        }
    };

    public static Readings of(Channel channel) {
        if (channel.equals(Channel.EMPTY_CHANNEL))
            return EMPTY_READINGS;

        return new OneChannelReadings(channel);
    }

    public static Readings of(Channel... channels) {
        if (channels.length == 0)
            throw new IllegalArgumentException("0 length channels not supported");

        if (channels.length == 1)
            return of(channels[0]);

        return new MultiChannelReadings(channels);
    }

    public abstract int numChannels();

    /**
     * Gets the channel corresponding to the given number.
     * Note channels are 1 based, so you can't do a typical 0 based for loop getting channels.
     *
     * @param i the channel number
     * @return the channel for the given number
     */
    public abstract Channel channel(int i);

    @SuppressWarnings("WeakerAccess")
    public Stream<Channel> channelStream() {
        int numChannels = numChannels();
        return IntStream.rangeClosed(1, numChannels).mapToObj(this::channel);
    }

    @Override
    public abstract int length();

    @Override
    public double get(int i) {
        if (numChannels() == 1)
            return channel(1).get(i);
        else {
            double value = channel(1).get(i);
            for (int idx = 2, len = numChannels(); idx <= len; ++idx) {
                value += channel(idx).get(i);
            }
            return value;
        }
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof Readings)) return false;
        Readings that = (Readings) o;
        return Arrays.equals(channelStream().toArray(), that.channelStream().toArray());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(channelStream().toArray());
    }

    @Override
    public String toString() {
        String name = getClass().getSimpleName();
        if (name.equals(""))
            name = getClass().getName();

        return name + "{" +
            "channels=" + Arrays.toString(channelStream().toArray()) +
            '}';
    }

}
