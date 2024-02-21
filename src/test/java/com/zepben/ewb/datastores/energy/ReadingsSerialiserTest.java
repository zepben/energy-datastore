/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.ewb.datastores.energy;

import com.zepben.blobstore.BytesUtil;
import com.zepben.energy.model.Channel;
import com.zepben.energy.model.Readings;
import com.zepben.energy.model.ZeroedReadingsCache;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.zepben.energy.model.Matchers.hasEqualChannels;
import static com.zepben.testutils.exception.ExpectException.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ReadingsSerialiserTest {

    @Test
    public void serialiseDeserialise() {
        Channel[] channels = {Channel.of(-1.1, 0, 2.2), Channel.of(0.5, 33., 44.4), Channel.of(0, 0, 0)};
        Readings readings = Readings.of(channels);
        ReadingsSerialiser sx = new ReadingsSerialiser();
        byte[] bytes = sx.sx(readings);

        ByteBuffer buffer = ByteBuffer.allocate(32);
        buffer.put((byte) channels.length);
        buffer.putInt(channels[0].length());
        buffer.put((byte) 1);
        BytesUtil.INSTANCE.encode7BitLong(buffer, -1100L);
        BytesUtil.INSTANCE.encode7BitLong(buffer, 0L);
        BytesUtil.INSTANCE.encode7BitLong(buffer, 2200L);
        buffer.put((byte) 2);
        BytesUtil.INSTANCE.encode7BitLong(buffer, 500L);
        BytesUtil.INSTANCE.encode7BitLong(buffer, 33000L);
        BytesUtil.INSTANCE.encode7BitLong(buffer, 44400L);
        buffer.put((byte) -3);
        buffer.flip();

        assertThat(sx.sxOffset(), is(0));
        bytes = Arrays.copyOf(bytes, sx.sxLength());
        byte[] expectedBytes = Arrays.copyOf(buffer.array(), buffer.limit());
        assertThat(bytes, equalTo(expectedBytes));

        ReadingsDeserialiser dsx = new ReadingsDeserialiser(EwbChannelFactory.DOUBLE_VALUES);
        Readings dsxReadings = dsx.dsx(bytes);
        assertThat(dsxReadings, notNullValue());
        assertThat(dsxReadings, hasEqualChannels(readings));
    }

    @Test
    public void doesNotSupportNaN() {
        Readings readings = Readings.of(Channel.of(Double.NaN));
        ReadingsSerialiser sx = new ReadingsSerialiser();
        expect(() -> sx.sx(readings))
            .toThrow(IllegalArgumentException.class)
            .withMessage("NaN values are not supported");
    }

    @Test
    public void maxChannelsIs127() {
        Channel[] channels = new Channel[128];
        for (int i = 0; i < channels.length; ++i)
            channels[i] = Channel.of(i);

        Readings readings = Readings.of(channels);
        ReadingsSerialiser sx = new ReadingsSerialiser();
        expect(() -> sx.sx(readings))
            .toThrow(IllegalArgumentException.class)
            .withMessage("the maximum number of channels supported is 127");
    }

    @Test
    public void zeroLengthChannelsUsesEmptyReadingsInstance() {
        ReadingsSerialiser sx = new ReadingsSerialiser();
        byte[] bytes = sx.sx(Readings.EMPTY_READINGS);
        assertThat(sx.sxOffset(), is(0));
        assertThat(sx.sxLength(), is(6));

        byte[] expectedBytes = new byte[]{1, 0, 0, 0, 0, -1};
        assertThat(Arrays.copyOf(bytes, sx.sxLength()), equalTo(expectedBytes));

        ReadingsDeserialiser dsx = new ReadingsDeserialiser(EwbChannelFactory.DOUBLE_VALUES);
        Readings dsxReadings = dsx.dsx(bytes);
        assertThat(dsxReadings, sameInstance(ZeroedReadingsCache.of(1, 0)));
    }

    @Test
    public void allZeroedChannelsUsesCache() {
        final int readingsLength = 48;
        final int numChannels = 2;
        ReadingsSerialiser sx = new ReadingsSerialiser();
        byte[] bytes = sx.sx(ZeroedReadingsCache.of(numChannels, readingsLength));
        assertThat(sx.sxOffset(), is(0));
        assertThat(sx.sxLength(), is(7));

        ByteBuffer buffer = ByteBuffer.allocate(7);
        buffer.put((byte) numChannels);
        buffer.putInt(readingsLength);
        buffer.put((byte) -1);
        buffer.put((byte) -2);
        assertThat(Arrays.copyOf(bytes, 7), equalTo(buffer.array()));

        ReadingsDeserialiser dsx = new ReadingsDeserialiser(EwbChannelFactory.DOUBLE_VALUES);
        Readings dsxReadings = dsx.dsx(bytes);
        assertThat(dsxReadings, sameInstance(ZeroedReadingsCache.of(numChannels, readingsLength)));
    }

    @Test
    public void serialiseNotANumberThrows() {
        ReadingsSerialiser sx = new ReadingsSerialiser();
        expect(() -> sx.sx(Readings.of(Channel.of(Double.NaN)))).toThrow(IllegalArgumentException.class);
    }

    @Test
    public void deserialiseTooShortBufferReturnsNull() {
        ReadingsDeserialiser dsx = new ReadingsDeserialiser(EwbChannelFactory.DOUBLE_VALUES);
        assertThat(dsx.dsx(new byte[]{0}), equalTo(null));
        assertThat(dsx.dsx(new byte[16], 15, 4), equalTo(null));
    }

    @Test
    public void deserialiseBufferUnderflowReturnsNull() {
        byte[] bytes = new byte[]{0, 0, 0, 4, 1};
        ReadingsDeserialiser dsx = new ReadingsDeserialiser(EwbChannelFactory.DOUBLE_VALUES);
        assertThat(dsx.dsx(bytes), equalTo(null));
    }

    @Test
    public void deserialiseGrowsBuffer() {
        double[] values = new double[96];
        for (int i = 0; i < values.length; ++i)
            values[i] = i;

        Readings readings = Readings.of(Channel.of(values));
        ReadingsSerialiser sx = new ReadingsSerialiser();
        byte[] bytes = sx.sx(readings);

        ReadingsDeserialiser dsx = new ReadingsDeserialiser(EwbChannelFactory.DOUBLE_VALUES);
        Readings dsxReadings = dsx.dsx(bytes, sx.sxOffset(), sx.sxLength());
        assertNotNull(dsxReadings);
        assertThat(dsxReadings, equalTo(readings));
    }

}
