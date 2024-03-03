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

import com.v7878.foreign.Linker.Option;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

class _LinkerOptions {

    private static final _LinkerOptions EMPTY = new _LinkerOptions(Map.of());
    private final Map<Class<?>, LinkerOptionImpl> optionsMap;

    private _LinkerOptions(Map<Class<?>, LinkerOptionImpl> optionsMap) {
        this.optionsMap = optionsMap;
    }

    public static _LinkerOptions forDowncall(FunctionDescriptor desc, Option... options) {
        List<Option> optionsList = new ArrayList<>(List.of(options));
        if (desc.returnLayout().filter(layout -> layout instanceof GroupLayout).isPresent()) {
            optionsList.add(ReturnInMemory.INSTANCE);
        }
        return forShared(LinkerOptionImpl::validateForDowncall, desc, optionsList);
    }

    public static _LinkerOptions forUpcall(FunctionDescriptor desc, Option[] options) {
        return forShared(LinkerOptionImpl::validateForUpcall, desc, List.of(options));
    }

    private static _LinkerOptions forShared(BiConsumer<LinkerOptionImpl, FunctionDescriptor> validator,
                                            FunctionDescriptor desc, List<Option> options) {
        Map<Class<?>, LinkerOptionImpl> optionMap = new HashMap<>();

        for (Option option : options) {
            if (optionMap.containsKey(option.getClass())) {
                throw new IllegalArgumentException("Duplicate option: " + option);
            }
            LinkerOptionImpl opImpl = (LinkerOptionImpl) option;
            validator.accept(opImpl, desc);
            optionMap.put(option.getClass(), opImpl);
        }

        _LinkerOptions linkerOptions = new _LinkerOptions(optionMap);
        if (linkerOptions.hasCapturedCallState() && linkerOptions.isCritical()) {
            throw new IllegalArgumentException("Incompatible linker options: captureCallState, critical");
        }
        return linkerOptions;
    }

    public static _LinkerOptions empty() {
        return EMPTY;
    }

    private <T extends Option> T getOption(Class<T> type) {
        return type.cast(optionsMap.get(type));
    }

    public boolean isVarargsIndex(int argIndex) {
        FirstVariadicArg fva = getOption(FirstVariadicArg.class);
        return fva != null && argIndex >= fva.index();
    }

    public boolean isReturnInMemory() {
        return getOption(ReturnInMemory.class) != null;
    }

    public boolean hasCapturedCallState() {
        return getOption(CaptureCallState.class) != null;
    }

    public Stream<_CapturableState> capturedCallState() {
        CaptureCallState stl = getOption(CaptureCallState.class);
        return stl == null ? Stream.empty() : stl.saved().stream();
    }

    public boolean isVariadicFunction() {
        FirstVariadicArg fva = getOption(FirstVariadicArg.class);
        return fva != null;
    }

    public int firstVariadicArgIndex() {
        return getOption(FirstVariadicArg.class).index();
    }

    public boolean isCritical() {
        Critical c = getOption(Critical.class);
        return c != null;
    }

    public boolean allowsHeapAccess() {
        Critical c = getOption(Critical.class);
        return c != null && c.allowHeapAccess();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof _LinkerOptions that
                && Objects.equals(optionsMap, that.optionsMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(optionsMap);
    }

    public sealed interface LinkerOptionImpl extends Option
            permits CaptureCallState, Critical, FirstVariadicArg, ReturnInMemory {
        default void validateForDowncall(FunctionDescriptor descriptor) {
            throw new IllegalArgumentException("Not supported for downcall: " + this);
        }

        default void validateForUpcall(FunctionDescriptor descriptor) {
            throw new IllegalArgumentException("Not supported for upcall: " + this);
        }
    }

    public static final class FirstVariadicArg implements LinkerOptionImpl {
        private final int index;

        public FirstVariadicArg(int index) {
            this.index = index;
        }

        @Override
        public void validateForDowncall(FunctionDescriptor descriptor) {
            if (index < 0 || index > descriptor.argumentLayouts().size()) {
                throw new IllegalArgumentException("Index '" + index + "' not in bounds for descriptor: " + descriptor);
            }
        }

        public int index() {
            return index;
        }
    }

    public static final class ReturnInMemory implements LinkerOptionImpl {
        public static final ReturnInMemory INSTANCE = new ReturnInMemory();

        private ReturnInMemory() {
        }

        @Override
        public void validateForDowncall(FunctionDescriptor descriptor) {
            // always allowed
        }
    }

    public static final class CaptureCallState implements LinkerOptionImpl {
        private final Set<_CapturableState> saved;

        public CaptureCallState(Set<_CapturableState> saved) {
            this.saved = saved;
        }

        @Override
        public void validateForDowncall(FunctionDescriptor descriptor) {
            // done during construction
        }

        public Set<_CapturableState> saved() {
            return saved;
        }
    }

    public enum Critical implements LinkerOptionImpl {
        ALLOW_HEAP(true),
        DONT_ALLOW_HEAP(false);

        private final boolean allowHeapAccess;

        Critical(boolean allowHeapAccess) {
            this.allowHeapAccess = allowHeapAccess;
        }

        @Override
        public void validateForDowncall(FunctionDescriptor descriptor) {
            // always allowed
        }

        public boolean allowHeapAccess() {
            return allowHeapAccess;
        }
    }
}
