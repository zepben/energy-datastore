/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.model;

import com.zepben.annotations.EverythingIsNonnullByDefault;

/**
 * This is a marker class to be able to check if readings were missing by using instanceof.
 */
@EverythingIsNonnullByDefault
public class MissingReadings extends ZeroedReadings {

    MissingReadings(int length) {
        super(1, length);
    }

}
