/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.datastore.blobstore;

import com.zepben.annotations.EverythingIsNonnullByDefault;

import javax.annotation.Nullable;

@EverythingIsNonnullByDefault
public interface Deserialiser<T> {

    /**
     * Deserialise an item from a byte array
     *
     * @param bytes a byte array that is the serialised item
     * @return the deserialised item.
     */
    @Nullable
    default T dsx(byte[] bytes) {
        return dsx(bytes, 0, bytes.length);
    }

    /**
     * Deserialise an item from a byte array at the given offset with the given length
     *
     * @param bytes  the byte array that holds the serialised item
     * @param offset the offset into the byte array to start deserialisation from
     * @param length the number of bytes in the byte array that belong to this deserialisation call
     * @return the deserialised item, or null if deserialisation failed
     */
    @Nullable
    T dsx(byte[] bytes, int offset, int length);

}
