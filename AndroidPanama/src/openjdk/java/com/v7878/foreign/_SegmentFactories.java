/*
 * Copyright (c) 2023 - 2025 Oracle and/or its affiliates. All rights reserved.
 * Modifications Copyright (c) 2025 Vladimir Kozelkov.
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

import com.v7878.foreign._HeapMemorySegmentImpl.OfByte;
import com.v7878.foreign._HeapMemorySegmentImpl.OfChar;
import com.v7878.foreign._HeapMemorySegmentImpl.OfDouble;
import com.v7878.foreign._HeapMemorySegmentImpl.OfFloat;
import com.v7878.foreign._HeapMemorySegmentImpl.OfInt;
import com.v7878.foreign._HeapMemorySegmentImpl.OfLong;
import com.v7878.foreign._HeapMemorySegmentImpl.OfObject;
import com.v7878.foreign._HeapMemorySegmentImpl.OfShort;
import com.v7878.foreign._MemorySessionImpl.ResourceList.ResourceCleanup;
import com.v7878.r8.annotations.AlwaysInline;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.Utils;
import com.v7878.unsafe.VM;
import com.v7878.unsafe.access.JavaNioAccess.UnmapperProxy;

import java.util.Objects;

/**
 * This class is used to retrieve concrete memory segment implementations, while making sure that classes
 * are initialized in the right order (that is, that {@code MemorySegment} is always initialized first).
 */
final class _SegmentFactories {

    // The maximum alignment supported by malloc - typically 16 bytes on
    // 64-bit platforms and 8 bytes on 32-bit platforms.
    private static final long MAX_MALLOC_ALIGN = AndroidUnsafe.IS64BIT ? 16 : 8;

    // Unsafe native segment factories. These are used by the implementation code, to skip the sanity checks
    // associated with MemorySegment::ofAddress.

    public static _NativeMemorySegmentImpl makeNativeSegmentUnchecked(
            long min, long byteSize, _MemorySessionImpl sessionImpl,
            boolean readOnly, Runnable action) {
        if (action == null) {
            sessionImpl.checkValidState();
        } else {
            sessionImpl.addCloseAction(action);
        }
        return new _NativeMemorySegmentImpl(min, byteSize, readOnly, sessionImpl);
    }

    public static _NativeMemorySegmentImpl makeNativeSegmentUnchecked(
            long min, long byteSize, _MemorySessionImpl sessionImpl, Runnable action) {
        return makeNativeSegmentUnchecked(min, byteSize, sessionImpl, false, action);
    }

    public static _NativeMemorySegmentImpl makeNativeSegmentUnchecked(
            long min, long byteSize, _MemorySessionImpl sessionImpl) {
        return makeNativeSegmentUnchecked(min, byteSize, sessionImpl, null);
    }

    public static _NativeMemorySegmentImpl makeNativeSegmentUnchecked(long min, long byteSize) {
        return makeNativeSegmentUnchecked(min, byteSize, _GlobalSession.INSTANCE);
    }

    // Port-added
    public static OfObject fromObject(Object obj) {
        Objects.requireNonNull(obj);
        return new OfObject(0, obj, VM.alignedSizeOf(obj), false,
                _MemorySessionImpl.createGlobalHolder(obj));
    }

    public static OfByte fromArray(byte[] arr) {
        Objects.requireNonNull(arr);
        long byteSize = (long) arr.length * _Utils.BaseAndScale.BYTE.scale();
        return new OfByte(_Utils.BaseAndScale.BYTE.base(), arr, byteSize, false,
                _MemorySessionImpl.createGlobalHolder(arr));
    }

    public static OfShort fromArray(short[] arr) {
        Objects.requireNonNull(arr);
        long byteSize = (long) arr.length * _Utils.BaseAndScale.SHORT.scale();
        return new OfShort(_Utils.BaseAndScale.SHORT.base(), arr, byteSize, false,
                _MemorySessionImpl.createGlobalHolder(arr));
    }

    public static OfInt fromArray(int[] arr) {
        Objects.requireNonNull(arr);
        long byteSize = (long) arr.length * _Utils.BaseAndScale.INT.scale();
        return new OfInt(_Utils.BaseAndScale.INT.base(), arr, byteSize, false,
                _MemorySessionImpl.createGlobalHolder(arr));
    }

    public static OfChar fromArray(char[] arr) {
        Objects.requireNonNull(arr);
        long byteSize = (long) arr.length * _Utils.BaseAndScale.CHAR.scale();
        return new OfChar(_Utils.BaseAndScale.CHAR.base(), arr, byteSize, false,
                _MemorySessionImpl.createGlobalHolder(arr));
    }

    public static OfFloat fromArray(float[] arr) {
        Objects.requireNonNull(arr);
        long byteSize = (long) arr.length * _Utils.BaseAndScale.FLOAT.scale();
        return new OfFloat(_Utils.BaseAndScale.FLOAT.base(), arr, byteSize, false,
                _MemorySessionImpl.createGlobalHolder(arr));
    }

    public static OfDouble fromArray(double[] arr) {
        Objects.requireNonNull(arr);
        long byteSize = (long) arr.length * _Utils.BaseAndScale.DOUBLE.scale();
        return new OfDouble(_Utils.BaseAndScale.DOUBLE.base(), arr, byteSize, false,
                _MemorySessionImpl.createGlobalHolder(arr));
    }

    public static OfLong fromArray(long[] arr) {
        Objects.requireNonNull(arr);
        long byteSize = (long) arr.length * _Utils.BaseAndScale.LONG.scale();
        return new OfLong(_Utils.BaseAndScale.LONG.base(), arr, byteSize, false,
                _MemorySessionImpl.createGlobalHolder(arr));
    }

    // Buffer conversion factories
    public static OfByte arrayOfByteSegment(Object base, long offset, long length,
                                            boolean readOnly, _MemorySessionImpl bufferScope) {
        return new OfByte(offset, base, length, readOnly, bufferScope);
    }

    public static OfShort arrayOfShortSegment(Object base, long offset, long length,
                                              boolean readOnly, _MemorySessionImpl bufferScope) {
        return new OfShort(offset, base, length, readOnly, bufferScope);
    }

    public static OfChar arrayOfCharSegment(Object base, long offset, long length,
                                            boolean readOnly, _MemorySessionImpl bufferScope) {
        return new OfChar(offset, base, length, readOnly, bufferScope);
    }

    public static OfInt arrayOfIntSegment(Object base, long offset, long length,
                                          boolean readOnly, _MemorySessionImpl bufferScope) {
        return new OfInt(offset, base, length, readOnly, bufferScope);
    }

    public static OfFloat arrayOfFloatSegment(Object base, long offset, long length,
                                              boolean readOnly, _MemorySessionImpl bufferScope) {
        return new OfFloat(offset, base, length, readOnly, bufferScope);
    }

    public static OfLong arrayOfLongSegment(Object base, long offset, long length,
                                            boolean readOnly, _MemorySessionImpl bufferScope) {
        return new OfLong(offset, base, length, readOnly, bufferScope);
    }

    public static OfDouble arrayOfDoubleSegment(Object base, long offset, long length,
                                                boolean readOnly, _MemorySessionImpl bufferScope) {
        return new OfDouble(offset, base, length, readOnly, bufferScope);
    }

    public static _NativeMemorySegmentImpl allocateNativeSegment(
            long byteSize, long byteAlignment, _MemorySessionImpl sessionImpl, boolean init) {
        long address = allocateNativeInternal(byteSize, byteAlignment, sessionImpl, init);
        return new _NativeMemorySegmentImpl(address, byteSize, false, sessionImpl);
    }

    private static long allocateNativeInternal(long byteSize, long byteAlignment,
                                               _MemorySessionImpl sessionImpl, boolean init) {
        _Utils.checkAllocationSizeAndAlign(byteSize, byteAlignment);
        sessionImpl.checkValidState();

        // TODO?
        //if (VM.isDirectMemoryPageAligned()) {
        //    byteAlignment = Math.max(byteAlignment, AndroidUnsafe.pageSize());
        //}

        // Check for wrap around
        if (byteSize < 0) {
            throw new OutOfMemoryError();
        }
        // Always allocate at least some memory so that zero-length segments have distinct
        // non-zero addresses.
        byteSize = Math.max(1, byteSize);

        long allocationSize;
        long allocationBase;
        long result;

        if (byteAlignment > MAX_MALLOC_ALIGN) {
            allocationSize = byteSize + byteAlignment - MAX_MALLOC_ALIGN;

            allocationBase = allocateMemoryWrapper(allocationSize);
            result = _Utils.alignUp(allocationBase, byteAlignment);
        } else {
            allocationSize = byteSize;

            allocationBase = allocateMemoryWrapper(allocationSize);
            result = allocationBase;
        }

        if (init) {
            initNativeMemory(result, byteSize);
        }

        sessionImpl.addOrCleanupIfFail(new ResourceCleanup() {
            @Override
            public void cleanup() {
                AndroidUnsafe.freeMemory(allocationBase);
            }
        });
        return result;
    }

    @AlwaysInline
    private static void initNativeMemory(long address, long byteSize) {
        AndroidUnsafe.setMemory(address, byteSize, (byte) 0);
    }

    @AlwaysInline
    private static long allocateMemoryWrapper(long size) {
        try {
            return AndroidUnsafe.allocateMemory(size);
        } catch (IllegalArgumentException ex) {
            throw new OutOfMemoryError();
        }
    }

    public static _MappedMemorySegmentImpl mapSegment(UnmapperProxy unmapper, long size,
                                                      boolean readOnly, _MemorySessionImpl sessionImpl) {
        if (unmapper == null) {
            return new _MappedMemorySegmentImpl(0, UnmapperProxy.DUMMY, 0, readOnly, sessionImpl);
        }
        sessionImpl.checkValidState();
        _MappedMemorySegmentImpl segment = new _MappedMemorySegmentImpl(
                unmapper.address(), unmapper, size, readOnly, sessionImpl);
        sessionImpl.addOrCleanupIfFail(new ResourceCleanup() {
            @Override
            public void cleanup() {
                unmapper.unmap();
            }
        });
        return segment;
    }

    // The method below needs to be called before any concrete subclass of MemorySegment
    // is instantiated. This is to make sure that we cannot have an initialization deadlock
    // where one thread attempts to initialize e.g. MemorySegment (and then NativeMemorySegmentImpl, via
    // the MemorySegment.NULL field) while another thread is attempting to initialize
    // NativeMemorySegmentImpl (and then MemorySegment, the super-interface).
    static {
        Utils.reachabilityFence(MemorySegment.NULL);
    }
}
