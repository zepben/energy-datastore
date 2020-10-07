/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.datastore;

import javax.annotation.Nullable;
import java.time.LocalDate;

/**
 * Handler for when there is an error reading or writing data from a store.
 */
@FunctionalInterface
public interface ErrorHandler {

    void handle(String id,
                LocalDate date,
                String msg,
                @Nullable Throwable t);

}
