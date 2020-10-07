/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.datastore;

import com.zepben.annotations.EverythingIsNonnullByDefault;

import java.time.LocalDate;

/**
 * Handler to handle items once they are read from a store.
 */
@FunctionalInterface
@EverythingIsNonnullByDefault
public interface ItemHandler<T> {

    void handle(String id, LocalDate date, T item);

}
