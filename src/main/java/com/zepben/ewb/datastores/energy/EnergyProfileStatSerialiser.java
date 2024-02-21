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
import com.zepben.energy.model.EnergyProfileStat;

import java.nio.ByteBuffer;

@EverythingIsNonnullByDefault
public class EnergyProfileStatSerialiser implements Serialiser<EnergyProfileStat> {

    private static final int NUM_STAT_VALUES = 3;

    // Buffer has length of the number of stat values times the maximum 7 bit encoded long
    private final ByteBuffer buffer = ByteBuffer.allocate(NUM_STAT_VALUES * 9);

    @Override
    public byte[] sx(EnergyProfileStat stat) {
        buffer.clear();

        BytesUtil.INSTANCE.encode7BitLong(buffer, KToUnitCodec.kToUnit(stat.kwIn()));
        BytesUtil.INSTANCE.encode7BitLong(buffer, KToUnitCodec.kToUnit(stat.kwOut()));
        BytesUtil.INSTANCE.encode7BitLong(buffer, KToUnitCodec.kToUnit(stat.kwNet()));

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

}
