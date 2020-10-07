/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.datastore.blobstore;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum EnergyProfileAttribute {
    KW_IN("W_in"),
    KW_OUT("W_out"),
    MAXIMUMS("maximums"),
    CACHEABLE("cacheable");

    private static final Set<String> tagSet;

    static {
        Set<String> tags = new HashSet<>();
        for (EnergyProfileAttribute tag : EnergyProfileAttribute.values()) {
            tags.add(tag.storeString());
        }
        tagSet = Collections.unmodifiableSet(tags);
    }

    private final String tag;

    EnergyProfileAttribute(String tag) {
        this.tag = tag;
    }

    public String storeString() {
        return tag;
    }

    public static Set<String> storeTagSet() {
        return tagSet;
    }

}
