/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.ewb.datastores.energy;

import com.zepben.blobstore.BytesUtil;
import com.zepben.energy.model.EnergyProfileStat;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.zepben.testutils.exception.ExpectException.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class EnergyProfileStatSerialiserTest {

    private EnergyProfileStatSerialiser sx = new EnergyProfileStatSerialiser();
    private EnergyProfileStatDeserialiser dsx = new EnergyProfileStatDeserialiser();

    @Test
    public void serialiseDeserialise() {
        final double kwIn = 3.3;
        final double kwOut = 2.2;
        final double kwNet = 1.1;
        EnergyProfileStat stat = new EnergyProfileStat(kwIn, kwOut, kwNet);
        byte[] bytes = sx.sx(stat);

        ByteBuffer buffer = ByteBuffer.allocate(6);
        BytesUtil.encode7BitLong(buffer, KToUnitCodec.kToUnit(kwIn));
        BytesUtil.encode7BitLong(buffer, KToUnitCodec.kToUnit(kwOut));
        BytesUtil.encode7BitLong(buffer, KToUnitCodec.kToUnit(kwNet));
        buffer.flip();

        assertThat(sx.sxOffset(), is(0));
        bytes = Arrays.copyOf(bytes, sx.sxLength());
        byte[] expectedBytes = Arrays.copyOf(buffer.array(), buffer.limit());
        assertThat(bytes, equalTo(expectedBytes));

        EnergyProfileStat dsxStat = dsx.dsx(bytes);
        assertThat(dsxStat, notNullValue());
        assertThat(dsxStat.kwIn(), equalTo(stat.kwIn()));
        assertThat(dsxStat.kwOut(), equalTo(stat.kwOut()));
        assertThat(dsxStat.kwNet(), equalTo(stat.kwNet()));
    }

//    @Test
//    public void doesNotSupportNaN() {
//        {
//            EnergyProfileStat stat = new EnergyProfileStat(Double.NaN, 1, 1);
//            expect(() -> sx.sx(stat))
//                .toThrow(IllegalArgumentException.class)
//                .withMessage("NaN values are not supported");
//        }
//
//        {
//            EnergyProfileStat stat = new EnergyProfileStat(1, Double.NaN, 1);
//            expect(() -> sx.sx(stat))
//                .toThrow(IllegalArgumentException.class)
//                .withMessage("NaN values are not supported");
//        }
//
//        {
//            EnergyProfileStat stat = new EnergyProfileStat(1, 1, Double.NaN);
//            expect(() -> sx.sx(stat))
//                .toThrow(IllegalArgumentException.class)
//                .withMessage("NaN values are not supported");
//        }
//    }

    @Test
    public void deserialiseTooShortBufferReturnsNull() {
        assertThat(dsx.dsx(new byte[]{0}), equalTo(null));
        assertThat(dsx.dsx(new byte[16], 15, 4), equalTo(null));
    }

//    @Test
//    public void deserialiseBufferUnderflowReturnsNull() {
//        byte[] bytes = new byte[] {0, 0, 0, 4, 1};
//        assertThat(dsx.dsx(bytes), equalTo(null));
//    }
}
