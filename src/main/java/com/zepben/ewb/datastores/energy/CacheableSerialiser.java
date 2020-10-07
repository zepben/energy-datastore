/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.ewb.datastores.energy;

import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.energy.datastore.blobstore.Serialiser;

@EverythingIsNonnullByDefault
class CacheableSerialiser implements Serialiser<Boolean> {

    private final byte[] trueValue = new byte[]{1};
    private final byte[] falseValue = new byte[]{0};

    @Override
    public byte[] sx(Boolean item) {
        if (item) {
            // Just in case some did something stupid and faffed with the value
            if (trueValue[0] != 1)
                trueValue[0] = 1;
            return trueValue;
        } else {
            // Just in case some did something stupid and faffed with the value
            if (falseValue[0] != 0)
                falseValue[0] = 0;
            return falseValue;
        }
    }

    @Override
    public int sxOffset() {
        return 0;
    }

    @Override
    public int sxLength() {
        return 1;
    }

}
