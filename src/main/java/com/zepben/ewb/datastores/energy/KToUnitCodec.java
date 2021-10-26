/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.ewb.datastores.energy;

import com.zepben.annotations.EverythingIsNonnullByDefault;

@EverythingIsNonnullByDefault
class KToUnitCodec {

    static long kToUnit(double v) {
        if (Double.isNaN(v))
            return Integer.MIN_VALUE;

        if (v == 0)
            return 0;

        return Math.round(v * 1000);
    }

    static double unitToK(long v) {
        if (v == Integer.MIN_VALUE)
            return Double.NaN;

        return v / 1000.;
    }

}
