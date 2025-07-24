/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

// Port-changed: Extensive modifications made throughout the class for Android.

package com.v7878.foreign;

import static com.v7878.foreign.ValueLayout.JAVA_INT;

import com.v7878.unsafe.foreign.Errno;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

enum _CapturableState {
    GET_LAST_ERROR("GetLastError", JAVA_INT, 1, false),
    WSA_GET_LAST_ERROR("WSAGetLastError", JAVA_INT, 1 << 1, false),
    ERRNO("errno", JAVA_INT, 1 << 2, true);

    public static final StructLayout LAYOUT = MemoryLayout.structLayout(
            supportedStates().map(_CapturableState::layout).toArray(MemoryLayout[]::new));
    public static final List<_CapturableState> BY_ORDINAL = List.of(values());

    static {
        assert (BY_ORDINAL.size() < Integer.SIZE); // Update LinkerOptions.CaptureCallState
    }

    static {
        // Init errno
        Errno.errno();
    }

    private final String stateName;
    private final ValueLayout layout;
    private final int mask;
    private final boolean isSupported;

    _CapturableState(String stateName, ValueLayout layout, int mask, boolean isSupported) {
        this.stateName = stateName;
        this.layout = layout.withName(stateName);
        this.mask = mask;
        this.isSupported = isSupported;
    }

    private static Stream<_CapturableState> supportedStates() {
        return Stream.of(values()).filter(_CapturableState::isSupported);
    }

    public static _CapturableState forName(String name) {
        return Stream.of(values())
                .filter(stl -> stl.stateName().equals(name))
                .filter(_CapturableState::isSupported)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown name: " + name + ", must be one of: "
                                + supportedStates()
                                .map(_CapturableState::stateName)
                                .collect(Collectors.joining(", "))));
    }

    public String stateName() {
        return stateName;
    }

    public ValueLayout layout() {
        return layout;
    }

    public int mask() {
        return mask;
    }

    public boolean isSupported() {
        return isSupported;
    }
}
