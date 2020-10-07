/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.datastore.blobstore;

import com.zepben.annotations.EverythingIsNonnullByDefault;

/**
 * Serialise instances of an item.
 * This interface allows implementors to reuse an underlying buffer by providing
 * a methods to get the offset and length into the buffered returned by the serialise method.
 */
@EverythingIsNonnullByDefault
public interface Serialiser<T> {

    /**
     * Serialises a readings instance into a byte array.
     *
     * @param item the item to serialise
     * @return a buffer containing
     */
    byte[] sx(T item);

    /**
     * Should return the offset into the byte array returned by the last call to {@link #sx}
     *
     * @return the offset into the byte array returned by the last serialisation.
     */
    int sxOffset();

    /**
     * Should return the length of the bytes for the last call to {@link #sx}
     *
     * @return the length of the bytes of the last serialisation.
     */
    int sxLength();

}
