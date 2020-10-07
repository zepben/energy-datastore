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
import com.zepben.energy.model.EnergyProfileStat;

import javax.annotation.Nullable;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

@EverythingIsNonnullByDefault
public class EnergyProfileStatDeserialiser implements Deserialiser<EnergyProfileStat> {

    private static final int NUM_STAT_VALUES = 3;

    // Buffer has length of the number of stat values times the maximum 7 bit encoded long
    private ByteBuffer buffer = ByteBuffer.allocate(NUM_STAT_VALUES * 9);

    @Nullable
    @Override
    public EnergyProfileStat dsx(byte[] bytes, int offset, int length) {
        buffer.clear();

        try {
            buffer.clear();
            buffer.put(bytes, offset, length);
            buffer.flip();

            double kwIn = KToUnitCodec.unitToK(BytesUtil.decode7BitLong(buffer));
            double kwOut = KToUnitCodec.unitToK(BytesUtil.decode7BitLong(buffer));
            double kwNet = KToUnitCodec.unitToK(BytesUtil.decode7BitLong(buffer));

            return new EnergyProfileStat(kwIn, kwOut, kwNet);// Need to remove id and date from profile stat class
        } catch (BufferUnderflowException | BufferOverflowException | IndexOutOfBoundsException e) {
            return null;
        }
    }

}
