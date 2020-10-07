/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.datastore.blobstore.indexing;

import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.energy.model.IdDateRange;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.time.LocalDate;

@EverythingIsNonnullByDefault
class IdDateRangeCodec {

    private final ByteBuffer buffer = ByteBuffer.allocate(12);

    @Nullable
    IdDateRange deserialise(String id, @Nullable byte[] bytes) {
        if (bytes == null || bytes.length < buffer.capacity())
            return null;

        buffer.clear();
        buffer.put(bytes);
        buffer.flip();

        LocalDate from = LocalDate.of(buffer.getInt(), buffer.get(), buffer.get());
        LocalDate to = LocalDate.of(buffer.getInt(), buffer.get(), buffer.get());
        return new IdDateRange(id, from, to);
    }

    byte[] serialise(LocalDate from, LocalDate to) {
        buffer.clear();
        buffer.putInt(from.getYear())
            .put((byte) from.getMonthValue())
            .put((byte) from.getDayOfMonth())
            .putInt(to.getYear())
            .put((byte) to.getMonthValue())
            .put((byte) to.getDayOfMonth());
        return buffer.array();
    }

}
