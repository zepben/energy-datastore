/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.ewb.datastores.energy;

import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.blobstore.BytesUtil;
import com.zepben.energy.datastore.blobstore.Deserialiser;
import com.zepben.energy.model.Channel;
import com.zepben.energy.model.Readings;
import com.zepben.energy.model.ZeroedChannelsCache;
import com.zepben.energy.model.ZeroedReadingsCache;

import javax.annotation.Nullable;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Deserilases a {@link Readings} instance that has been serialised by {@link ReadingsSerialiser}.
 * <p>The 7 bit zig-zag encoded longs are decoded into a standard long and then divided by 1000 to give reading values.
 */
@EverythingIsNonnullByDefault
class ReadingsDeserialiser implements Deserialiser<Readings> {

    private final ChannelFactory channelFactory;
    private ByteBuffer buffer = ByteBuffer.allocate(0);
    private double[] values = new double[48];
    private Channel[] channels = new Channel[1];

    ReadingsDeserialiser(ChannelFactory channelFactory) {
        this.channelFactory = channelFactory;
    }

    @Override
    @Nullable
    public Readings dsx(byte[] bytes, int offset, int length) {
        try {
            checkOrGrowBuffer(length);
            buffer.clear();
            buffer.put(bytes, offset, length);
            buffer.flip();

            // Get the number of channels
            int nChannels = buffer.get();
            if (nChannels <= 0) {
                return null;
            }

            if (nChannels != channels.length)
                channels = new Channel[nChannels];

            int nIntervals = buffer.getInt();
            if (nIntervals != values.length) {
                values = new double[nIntervals];
            }

            boolean allZeroed = createChannels(buffer, channels, values);

            if (allZeroed)
                return ZeroedReadingsCache.of(channels.length, channels[0].length());
            if (channels.length == 1)
                return Readings.of(channels[0]);
            else
                return Readings.of(channels);
        } catch (BufferUnderflowException | BufferOverflowException | IndexOutOfBoundsException e) {
            return null;
        }
    }

    private boolean createChannels(ByteBuffer buffer,
                                   Channel[] channels,
                                   double[] values) {
        int nChannels = channels.length;
        int nIntervals = values.length;

        boolean allZeroed = true;
        for (int channelIdx = 0; channelIdx < nChannels; ++channelIdx) {
            int channelNum = buffer.get();

            Channel channel;
            // Negative channel number means that channel has all 0 values
            if (channelNum < 0) {
                channelNum = -channelNum;
                channel = ZeroedChannelsCache.of(nIntervals);
            } else {
                allZeroed = false;
                for (int interval = 0; interval < nIntervals; ++interval) {
                    double value = KToUnitCodec.unitToK(BytesUtil.decode7BitLong(buffer));
                    values[interval] = value;
                }
                channel = channelFactory.create(values);
            }
            channels[channelNum - 1] = channel;
        }

        return allZeroed;
    }

    private void checkOrGrowBuffer(int newSize) {
        if (newSize > buffer.capacity())
            buffer = ByteBuffer.allocate(newSize);
    }

}
