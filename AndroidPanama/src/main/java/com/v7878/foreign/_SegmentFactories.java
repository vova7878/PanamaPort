/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import androidx.annotation.Keep;

import com.v7878.foreign._HeapMemorySegmentImpl.OfByte;
import com.v7878.foreign._HeapMemorySegmentImpl.OfChar;
import com.v7878.foreign._HeapMemorySegmentImpl.OfDouble;
import com.v7878.foreign._HeapMemorySegmentImpl.OfFloat;
import com.v7878.foreign._HeapMemorySegmentImpl.OfInt;
import com.v7878.foreign._HeapMemorySegmentImpl.OfLong;
import com.v7878.foreign._HeapMemorySegmentImpl.OfObject;
import com.v7878.foreign._HeapMemorySegmentImpl.OfShort;
import com.v7878.foreign._MemorySessionImpl.ResourceList.ResourceCleanup;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.VM;
import com.v7878.unsafe.access.JavaNioAccess.UnmapperProxy;

import java.util.Objects;

/**
 * This class is used to retrieve concrete memory segment implementations, while making sure that classes
 * are initialized in the right order (that is, that {@code MemorySegment} is always initialized first).
 * See {@link _SegmentFactories#ensureInitialized()}.
 */
class _SegmentFactories {

    // The maximum alignment supported by malloc - typically 16 bytes on
    // 64-bit platforms and 8 bytes on 32-bit platforms.
    private static final long MAX_MALLOC_ALIGN = AndroidUnsafe.IS64BIT ? 16 : 8;

    // Unsafe native segment factories. These are used by the implementation code, to skip the sanity checks
    // associated with MemorySegment::ofAddress.

    public static MemorySegment makeNativeSegmentUnchecked(long min, long byteSize, boolean readOnly, _MemorySessionImpl sessionImpl, Runnable action) {
        ensureInitialized();
        if (action == null) {
            sessionImpl.checkValidState();
        } else {
            sessionImpl.addCloseAction(action);
        }
        return new _NativeMemorySegmentImpl(min, byteSize, readOnly, sessionImpl);
    }

    public static MemorySegment makeNativeSegmentUnchecked(long min, long byteSize, _MemorySessionImpl sessionImpl, Runnable action) {
        return makeNativeSegmentUnchecked(min, byteSize, false, sessionImpl, action);
    }

    public static MemorySegment makeNativeSegmentUnchecked(long min, long byteSize, _MemorySessionImpl sessionImpl) {
        return makeNativeSegmentUnchecked(min, byteSize, sessionImpl, null);
    }

    public static MemorySegment makeNativeSegmentUnchecked(long min, long byteSize) {
        return makeNativeSegmentUnchecked(min, byteSize, _GlobalSession.INSTANCE);
    }

    // Port-added
    public static MemorySegment fromObject(Object obj) {
        ensureInitialized();
        Objects.requireNonNull(obj);
        return new OfObject(0, obj, VM.alignedSizeOf(obj), false,
                _MemorySessionImpl.createHeap(obj));
    }

    public static MemorySegment fromArray(byte[] arr) {
        ensureInitialized();
        Objects.requireNonNull(arr);
        long byteSize = (long) arr.length * _Utils.BaseAndScale.BYTE.scale();
        return new OfByte(_Utils.BaseAndScale.BYTE.base(), arr, byteSize, false,
                _MemorySessionImpl.createHeap(arr));
    }

    public static MemorySegment fromArray(short[] arr) {
        ensureInitialized();
        Objects.requireNonNull(arr);
        long byteSize = (long) arr.length * _Utils.BaseAndScale.SHORT.scale();
        return new OfShort(_Utils.BaseAndScale.SHORT.base(), arr, byteSize, false,
                _MemorySessionImpl.createHeap(arr));
    }

    public static MemorySegment fromArray(int[] arr) {
        ensureInitialized();
        Objects.requireNonNull(arr);
        long byteSize = (long) arr.length * _Utils.BaseAndScale.INT.scale();
        return new OfInt(_Utils.BaseAndScale.INT.base(), arr, byteSize, false,
                _MemorySessionImpl.createHeap(arr));
    }

    public static MemorySegment fromArray(char[] arr) {
        ensureInitialized();
        Objects.requireNonNull(arr);
        long byteSize = (long) arr.length * _Utils.BaseAndScale.CHAR.scale();
        return new OfChar(_Utils.BaseAndScale.CHAR.base(), arr, byteSize, false,
                _MemorySessionImpl.createHeap(arr));
    }

    public static MemorySegment fromArray(float[] arr) {
        ensureInitialized();
        Objects.requireNonNull(arr);
        long byteSize = (long) arr.length * _Utils.BaseAndScale.FLOAT.scale();
        return new OfFloat(_Utils.BaseAndScale.FLOAT.base(), arr, byteSize, false,
                _MemorySessionImpl.createHeap(arr));
    }

    public static MemorySegment fromArray(double[] arr) {
        ensureInitialized();
        Objects.requireNonNull(arr);
        long byteSize = (long) arr.length * _Utils.BaseAndScale.DOUBLE.scale();
        return new OfDouble(_Utils.BaseAndScale.DOUBLE.base(), arr, byteSize, false,
                _MemorySessionImpl.createHeap(arr));
    }

    public static MemorySegment fromArray(long[] arr) {
        ensureInitialized();
        Objects.requireNonNull(arr);
        long byteSize = (long) arr.length * _Utils.BaseAndScale.LONG.scale();
        return new OfLong(_Utils.BaseAndScale.LONG.base(), arr, byteSize, false,
                _MemorySessionImpl.createHeap(arr));
    }

    public static MemorySegment allocateSegment(long byteSize, long byteAlignment, _MemorySessionImpl sessionImpl) {
        ensureInitialized();
        sessionImpl.checkValidState();
        //TODO
        //if (VM.isDirectMemoryPageAligned()) {
        //    byteAlignment = Math.max(byteAlignment, AndroidUnsafe.pageSize());
        //}
        long alignedSize = Math.max(1L, byteAlignment > MAX_MALLOC_ALIGN ?
                byteSize + (byteAlignment - 1) :
                byteSize);

        long buf = allocateMemoryWrapper(alignedSize);
        long alignedBuf = _Utils.alignUp(buf, byteAlignment);
        _AbstractMemorySegmentImpl segment = new _NativeMemorySegmentImpl(buf, alignedSize,
                false, sessionImpl);
        sessionImpl.addOrCleanupIfFail(new ResourceCleanup() {
            @Override
            public void cleanup() {
                AndroidUnsafe.freeMemory(buf);
            }
        });
        if (alignedSize != byteSize) {
            long delta = alignedBuf - buf;
            segment = segment.asSlice(delta, byteSize);
        }
        return segment;
    }

    private static long allocateMemoryWrapper(long size) {
        try {
            return AndroidUnsafe.allocateMemory(size);
        } catch (IllegalArgumentException ex) {
            throw new OutOfMemoryError();
        }
    }

    public static MemorySegment mapSegment(UnmapperProxy unmapper, long size, boolean readOnly, _MemorySessionImpl sessionImpl) {
        ensureInitialized();
        if (unmapper != null) {
            _AbstractMemorySegmentImpl segment = new _MappedMemorySegmentImpl(
                    unmapper.address(), unmapper, size, readOnly, sessionImpl);
            ResourceCleanup resource = new ResourceCleanup() {
                @Override
                public void cleanup() {
                    unmapper.unmap();
                }
            };
            sessionImpl.addOrCleanupIfFail(resource);
            return segment;
        } else {
            return new _MappedMemorySegmentImpl(0, null, 0, readOnly, sessionImpl);
        }
    }

    // The method below needs to be called before any concrete subclass of MemorySegment
    // is instantiated. This is to make sure that we cannot have an initialization deadlock
    // where one thread attempts to initialize e.g. MemorySegment (and then NativeMemorySegmentImpl, via
    // the MemorySegment.NULL field) while another thread is attempting to initialize
    // NativeMemorySegmentImpl (and then MemorySegment, the super-interface).
    //TODO?: check
    @Keep
    private static void ensureInitialized() {
        //noinspection unused
        MemorySegment segment = MemorySegment.NULL;
    }
}
