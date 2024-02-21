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
import com.zepben.energy.datastore.blobstore.Serialiser;
import com.zepben.energy.model.Channel;
import com.zepben.energy.model.Readings;

import java.nio.ByteBuffer;

/**
 * Serialises a {@link Readings} instance that keep its units in kilo (thousands).
 * <p>What this does is multiplies each reading value by 1000 and rounds to the nearest whole number.
 * This does mean fractional values at the single unit level are lost.
 * <p>Values are stored as 7 bit zig-zag encoded longs. See {@link BytesUtil#encode7BitLong(ByteBuffer, long)}.
 */
@EverythingIsNonnullByDefault
class ReadingsSerialiser implements Serialiser<Readings> {

    private ByteBuffer buffer = ByteBuffer.allocate(0);

    @Override
    public byte[] sx(Readings readings) {
        if (readings.numChannels() > 127)
            throw new IllegalArgumentException("the maximum number of channels supported is 127");

        checkOrGrowBuffer(calculateSize(readings));

        buffer.clear();
        buffer.put((byte) readings.numChannels());
        buffer.putInt(readings.length());

        for (int channelNum = 1; channelNum <= readings.numChannels(); ++channelNum) {
            Channel channel = readings.channel(channelNum);

            buffer.mark();
            buffer.put((byte) channelNum);

            boolean allZero = true;
            for (int i = 0, n = channel.length(); i < n; ++i) {
                long value = KToUnitCodec.kToUnit(channel.get(i));
                allZero &= value == 0;
                BytesUtil.INSTANCE.encode7BitLong(buffer, value);
            }

            // If the whole array was zero valued, we don't save all the values. We just store the channel number as a
            // negative to flag they are all 0.
            if (allZero) {
                buffer.reset();
                buffer.put((byte) -channelNum);
            }
        }

        buffer.flip();
        return buffer.array();
    }

    @Override
    public int sxOffset() {
        return buffer.position();
    }

    @Override
    public int sxLength() {
        return buffer.remaining();
    }

    private int calculateSize(Readings readings) {
        return 1 + // 1 byte for number of channels
            4 + // int for number of intervals on each channel
            readings.numChannels() + // byte for each channels number
            (calculateValuesSize(readings) * readings.numChannels()); // Maximum number of bytes required to store all the values
    }

    private int calculateValuesSize(Readings readings) {
        return (readings.length() * 9);
    }

    private void checkOrGrowBuffer(int newSize) {
        if (newSize > buffer.capacity())
            buffer = ByteBuffer.allocate(newSize);
    }

}
