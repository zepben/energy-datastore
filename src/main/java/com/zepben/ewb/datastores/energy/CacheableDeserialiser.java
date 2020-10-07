/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.ewb.datastores.energy;

import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.energy.datastore.blobstore.Deserialiser;

import javax.annotation.Nullable;

@EverythingIsNonnullByDefault
class CacheableDeserialiser implements Deserialiser<Boolean> {

    @Nullable
    @Override
    public Boolean dsx(byte[] bytes, int offset, int length) {
        if (offset + length > bytes.length)
            return null;

        return bytes[offset] != 0 ? Boolean.TRUE : Boolean.FALSE;
    }

}
