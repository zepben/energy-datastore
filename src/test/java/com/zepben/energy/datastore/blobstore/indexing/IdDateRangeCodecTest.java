/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.datastore.blobstore.indexing;

import com.zepben.energy.model.IdDateRange;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class IdDateRangeCodecTest {

    private final IdDateRangeCodec codec = new IdDateRangeCodec();

    @Test
    public void serialises() {
        LocalDate from = LocalDate.now(ZoneId.systemDefault());
        LocalDate to = from.plusDays(721);
        byte[] bytes = codec.serialise(from, to);

        ByteBuffer buffer = ByteBuffer.allocate(12);
        buffer.putInt(from.getYear())
            .put((byte) from.getMonthValue())
            .put((byte) from.getDayOfMonth())
            .putInt(to.getYear())
            .put((byte) to.getMonthValue())
            .put((byte) to.getDayOfMonth());

        assertThat(bytes, equalTo(buffer.array()));
    }

    @Test
    public void deserialisers() {
        LocalDate from = LocalDate.now(ZoneId.systemDefault());
        LocalDate to = from.plusDays(721);

        ByteBuffer buffer = ByteBuffer.allocate(12);
        buffer.putInt(from.getYear())
            .put((byte) from.getMonthValue())
            .put((byte) from.getDayOfMonth())
            .putInt(to.getYear())
            .put((byte) to.getMonthValue())
            .put((byte) to.getDayOfMonth());

        IdDateRange range = codec.deserialise("id", buffer.array());
        IdDateRange expectedRange = new IdDateRange("id", from, to);
        assertThat(range, equalTo(expectedRange));
    }

}
