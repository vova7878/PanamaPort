/*
 *  Copyright (c) 2019, 2023, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

// Port-changed: Extensive modifications made throughout the class for Android.

package com.v7878.foreign;

import static com.v7878.misc.Math.ceilDiv;
import static java.util.stream.Collectors.joining;

import com.v7878.invoke.VarHandle;
import com.v7878.unsafe.invoke.MethodHandlesFixes;
import com.v7878.unsafe.invoke.VarHandles;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * This class provide support for constructing layout paths; that is, starting from a root path (see {@link #rootPath(MemoryLayout)}),
 * a path can be constructed by selecting layout elements using the selector methods provided by this class
 * (see {@link #sequenceElement()}, {@link #sequenceElement(long)}, {@link #sequenceElement(long, long)}, {@link #groupElement(String)}).
 * Once a path has been fully constructed, clients can ask for the offset associated with the layout element selected
 * by the path (see {@link #offset}), or obtain var handle to access the selected layout element
 * given an address pointing to a segment associated with the root layout (see {@link #dereferenceHandle()}).
 */
class _LayoutPath {

    private static final long[] EMPTY_STRIDES = new long[0];
    private static final long[] EMPTY_BOUNDS = new long[0];
    private static final MethodHandle[] EMPTY_DEREF_HANDLES = new MethodHandle[0];

    private static final MethodHandle MH_ADD_SCALED_OFFSET;
    private static final MethodHandle MH_SLICE;
    private static final MethodHandle MH_SLICE_LAYOUT;
    private static final MethodHandle MH_CHECK_ALIGN;
    private static final MethodHandle MH_SEGMENT_RESIZE;
    private static final MethodHandle MH_ADD;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MH_ADD_SCALED_OFFSET = lookup.findStatic(_LayoutPath.class, "addScaledOffset",
                    MethodType.methodType(long.class, long.class, long.class, long.class, long.class));
            MH_SLICE = lookup.findVirtual(MemorySegment.class, "asSlice",
                    MethodType.methodType(MemorySegment.class, long.class, long.class));
            MH_SLICE_LAYOUT = lookup.findVirtual(MemorySegment.class, "asSlice",
                    MethodType.methodType(MemorySegment.class, long.class, MemoryLayout.class));
            MH_CHECK_ALIGN = lookup.findStatic(_LayoutPath.class, "checkAlign",
                    MethodType.methodType(void.class, MemorySegment.class, long.class, MemoryLayout.class));
            MH_SEGMENT_RESIZE = lookup.findStatic(_LayoutPath.class, "resizeSegment",
                    MethodType.methodType(MemorySegment.class, MemorySegment.class, MemoryLayout.class));
            MH_ADD = lookup.findStatic(Long.class, "sum",
                    MethodType.methodType(long.class, long.class, long.class));
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private final MemoryLayout layout;
    private final long offset;
    private final _LayoutPath enclosing;
    private final long[] strides;
    private final long[] bounds;
    private final MethodHandle[] derefAdapters;

    private _LayoutPath(MemoryLayout layout, long offset, long[] strides, long[] bounds, MethodHandle[] derefAdapters, _LayoutPath enclosing) {
        this.layout = layout;
        this.offset = offset;
        this.strides = strides;
        this.bounds = bounds;
        this.derefAdapters = derefAdapters;
        this.enclosing = enclosing;
    }

    // Layout path selector methods

    public _LayoutPath sequenceElement() {
        SequenceLayout seq = requireSequenceLayout();
        MemoryLayout elem = seq.elementLayout();
        return _LayoutPath.nestedPath(elem, offset, addStride(elem.byteSize()), addBound(seq.elementCount()), derefAdapters, this);
    }

    public _LayoutPath sequenceElement(long start, long step) {
        SequenceLayout seq = requireSequenceLayout();
        checkSequenceBounds(seq, start);
        MemoryLayout elem = seq.elementLayout();
        long elemSize = elem.byteSize();
        long nelems = step > 0 ?
                seq.elementCount() - start :
                start + 1;
        long maxIndex = ceilDiv(nelems, Math.abs(step));
        return _LayoutPath.nestedPath(elem, offset + (start * elemSize),
                addStride(elemSize * step), addBound(maxIndex), derefAdapters, this);
    }

    public _LayoutPath sequenceElement(long index) {
        SequenceLayout seq = requireSequenceLayout();
        checkSequenceBounds(seq, index);
        long elemSize = seq.elementLayout().byteSize();
        long elemOffset = elemSize * index;
        return _LayoutPath.nestedPath(seq.elementLayout(), offset + elemOffset, strides, bounds, derefAdapters, this);
    }

    public _LayoutPath groupElement(String name) {
        GroupLayout g = requireGroupLayout();
        long offset = 0;
        MemoryLayout elem = null;
        for (int i = 0; i < g.memberLayouts().size(); i++) {
            MemoryLayout l = g.memberLayouts().get(i);
            if (l.name().isPresent() &&
                    l.name().get().equals(name)) {
                elem = l;
                break;
            } else if (g instanceof StructLayout) {
                offset += l.byteSize();
            }
        }
        if (elem == null) {
            throw badLayoutPath(
                    String.format("cannot resolve '%s' in layout %s", name, breadcrumbs()));
        }
        return _LayoutPath.nestedPath(elem, this.offset + offset, strides, bounds, derefAdapters, this);
    }

    public _LayoutPath groupElement(long index) {
        GroupLayout g = requireGroupLayout();
        long elemSize = g.memberLayouts().size();
        long offset = 0;
        MemoryLayout elem = null;
        for (int i = 0; i <= index; i++) {
            if (i == elemSize) {
                throw badLayoutPath(
                        String.format("cannot resolve element %d in layout: %s", index, breadcrumbs()));
            }
            elem = g.memberLayouts().get(i);
            if (g instanceof StructLayout && i < index) {
                offset += elem.byteSize();
            }
        }
        return _LayoutPath.nestedPath(elem, this.offset + offset, strides, bounds, derefAdapters, this);
    }

    public _LayoutPath derefElement() {
        if (!(layout instanceof AddressLayout addressLayout) ||
                !addressLayout.targetLayout().isPresent()) {
            throw badLayoutPath(
                    String.format("Cannot dereference layout: %s", breadcrumbs()));
        }
        MemoryLayout derefLayout = addressLayout.targetLayout().get();
        MethodHandle handle = dereferenceHandle(false).toMethodHandle(VarHandle.AccessMode.GET);
        handle = MethodHandles.filterReturnValue(handle,
                MethodHandles.insertArguments(MH_SEGMENT_RESIZE, 1, derefLayout));
        return derefPath(derefLayout, handle, this);
    }

    private static MemorySegment resizeSegment(MemorySegment segment, MemoryLayout layout) {
        return _Utils.longToAddress(segment.address(), layout.byteSize(), layout.byteAlignment());
    }

    // Layout path projections

    public long offset() {
        return offset;
    }

    public VarHandle dereferenceHandle() {
        return dereferenceHandle(true);
    }

    public VarHandle dereferenceHandle(boolean adapt) {
        if (!(layout instanceof ValueLayout valueLayout)) {
            throw new IllegalArgumentException(
                    String.format("Path does not select a value layout: %s", breadcrumbs()));
        }

        // If we have an enclosing layout, drop the alignment check for the accessed element,
        // we check the root layout instead
        ValueLayout accessedLayout = enclosing != null ? valueLayout.withByteAlignment(1) : valueLayout;
        VarHandle handle = accessedLayout.varHandle();
        handle = VarHandles.collectCoordinates(handle, 1, offsetHandle());

        // we only have to check the alignment of the root layout for the first dereference we do,
        // as each dereference checks the alignment of the target address when constructing its segment
        // (see _Utils::longToAddress)
        //TODO!!!
        //if (derefAdapters.length == 0 && enclosing != null) {
        //    // insert align check for the root layout on the initial MS + offset
        //    List<Class<?>> coordinateTypes = handle.coordinateTypes();
        //    MethodHandle alignCheck = MethodHandles.insertArguments(MH_CHECK_ALIGN, 2, rootLayout());
        //    handle = VarHandles.collectCoordinates(handle, 0, alignCheck);
        //    int[] reorder = IntStream.concat(IntStream.of(0, 1), IntStream.range(0, coordinateTypes.size())).toArray();
        //    handle = VarHandles.permuteCoordinates(handle, coordinateTypes, reorder);
        //}

        if (adapt) {
            if (derefAdapters.length > 0) {
                // plug up the base offset if we have at least 1 enclosing dereference
                handle = VarHandles.insertCoordinates(handle, 1, 0);
            }
            for (int i = derefAdapters.length - 1; i >= 0; i--) {
                MethodHandle adapter = derefAdapters[i];
                // the first/outermost adapter will have a base offset coordinate, the rest are constant 0
                if (i > 0) {
                    // plug in a constant 0 base offset for all but the outermost access in a deref chain
                    adapter = MethodHandles.insertArguments(adapter, 1, 0);
                }
                handle = VarHandles.collectCoordinates(handle, 0, adapter);
            }
        }
        return handle;
    }

    private static long addScaledOffset(long base, long index, long stride, long bound) {
        Objects.checkIndex(index, bound);
        return base + (stride * index);
    }

    public MethodHandle offsetHandle() {
        MethodHandle mh = MethodHandles.insertArguments(MH_ADD, 0, offset);
        for (int i = strides.length - 1; i >= 0; i--) {
            MethodHandle collector = MethodHandles.insertArguments(MH_ADD_SCALED_OFFSET, 2, strides[i], bounds[i]);
            // (J, ...) -> J to (J, J, ...) -> J
            // i.e. new coord is prefixed. Last coord will correspond to innermost layout
            mh = MethodHandlesFixes.collectArguments(mh, 0, collector);
        }
        return mh;
    }

    private MemoryLayout rootLayout() {
        return enclosing != null ? enclosing.rootLayout() : this.layout;
    }

    public MethodHandle sliceHandle() {
        MethodHandle sliceHandle;
        if (enclosing != null) {
            // drop the alignment check for the accessed element, we check the root layout instead
            sliceHandle = MH_SLICE; // (MS, long, long) -> MS
            sliceHandle = MethodHandles.insertArguments(sliceHandle, 2, layout.byteSize()); // (MS, long) -> MS
        } else {
            sliceHandle = MH_SLICE_LAYOUT; // (MS, long, MemoryLayout) -> MS
            sliceHandle = MethodHandles.insertArguments(sliceHandle, 2, layout); // (MS, long) -> MS
        }
        sliceHandle = MethodHandlesFixes.collectArguments(sliceHandle, 1, offsetHandle()); // (MS, long, ...) -> MS

        if (enclosing != null) {
            // insert align check for the root layout on the initial MS + offset
            MethodType oldType = sliceHandle.type();
            MethodHandle alignCheck = MethodHandles.insertArguments(MH_CHECK_ALIGN, 2, rootLayout());
            sliceHandle = MethodHandlesFixes.collectArguments(sliceHandle, 0, alignCheck); // (MS, long, MS, long) -> MS
            int[] reorder = IntStream.concat(IntStream.of(0, 1), IntStream.range(0, oldType.parameterCount())).toArray();
            sliceHandle = MethodHandlesFixes.permuteArguments(sliceHandle, oldType, reorder); // (MS, long, ...) -> MS
        }

        return sliceHandle;
    }

    private static void checkAlign(MemorySegment segment, long offset, MemoryLayout constraint) {
        if (!((_AbstractMemorySegmentImpl) segment).isAlignedForElement(offset, constraint)) {
            throw new IllegalArgumentException(String.format(
                    "Target offset %d is incompatible with alignment constraint %d (of %s) for segment %s"
                    , offset, constraint.byteAlignment(), constraint, segment));
        }
    }

    public MemoryLayout layout() {
        return layout;
    }

    // Layout path construction

    public static _LayoutPath rootPath(MemoryLayout layout) {
        return new _LayoutPath(layout, 0L, EMPTY_STRIDES, EMPTY_BOUNDS, EMPTY_DEREF_HANDLES, null);
    }

    private static _LayoutPath nestedPath(MemoryLayout layout, long offset, long[] strides, long[] bounds, MethodHandle[] derefAdapters, _LayoutPath encl) {
        return new _LayoutPath(layout, offset, strides, bounds, derefAdapters, encl);
    }

    private static _LayoutPath derefPath(MemoryLayout layout, MethodHandle handle, _LayoutPath encl) {
        MethodHandle[] handles = Arrays.copyOf(encl.derefAdapters, encl.derefAdapters.length + 1);
        handles[encl.derefAdapters.length] = handle;
        return new _LayoutPath(layout, 0L, EMPTY_STRIDES, EMPTY_BOUNDS, handles, null);
    }

    // Helper methods

    private SequenceLayout requireSequenceLayout() {
        return requireLayoutType(SequenceLayout.class, "sequence");
    }

    private GroupLayout requireGroupLayout() {
        return requireLayoutType(GroupLayout.class, "group");
    }

    private <T extends MemoryLayout> T requireLayoutType(Class<T> layoutClass, String name) {
        if (!layoutClass.isAssignableFrom(layout.getClass())) {
            throw badLayoutPath(
                    String.format("attempting to select a %s element from a non-%s layout: %s",
                            name, name, breadcrumbs()));
        }
        return layoutClass.cast(layout);
    }

    private void checkSequenceBounds(SequenceLayout seq, long index) {
        if (index >= seq.elementCount()) {
            throw badLayoutPath(String.format("sequence index out of bounds; index: %d, elementCount is %d for layout %s",
                    index, seq.elementCount(), breadcrumbs()));
        }
    }

    private static IllegalArgumentException badLayoutPath(String cause) {
        return new IllegalArgumentException("Bad layout path: " + cause);
    }

    private long[] addStride(long stride) {
        long[] newStrides = Arrays.copyOf(strides, strides.length + 1);
        newStrides[strides.length] = stride;
        return newStrides;
    }

    private long[] addBound(long maxIndex) {
        long[] newBounds = Arrays.copyOf(bounds, bounds.length + 1);
        newBounds[bounds.length] = maxIndex;
        return newBounds;
    }

    private String breadcrumbs() {
        //TODO
        return Stream.iterate(this, Objects::nonNull, lp -> lp.enclosing)
                .map(_LayoutPath::layout)
                .map(Object::toString)
                .collect(joining(", selected from: "));
    }

    /**
     * This class provides an immutable implementation for the {@code PathElement} interface. A path element implementation
     * is simply a pointer to one of the selector methods provided by the {@code _LayoutPath} class.
     */
    public static final class PathElementImpl implements MemoryLayout.PathElement, UnaryOperator<_LayoutPath> {

        public enum PathKind {
            SEQUENCE_ELEMENT("unbound sequence element"),
            SEQUENCE_ELEMENT_INDEX("bound sequence element"),
            SEQUENCE_RANGE("sequence range"),
            GROUP_ELEMENT("group element"),
            DEREF_ELEMENT("dereference element");

            final String description;

            PathKind(String description) {
                this.description = description;
            }

            public String description() {
                return description;
            }
        }

        final PathKind kind;
        final UnaryOperator<_LayoutPath> pathOp;

        public PathElementImpl(PathKind kind, UnaryOperator<_LayoutPath> pathOp) {
            this.kind = kind;
            this.pathOp = pathOp;
        }

        @Override
        public _LayoutPath apply(_LayoutPath layoutPath) {
            return pathOp.apply(layoutPath);
        }

        public PathKind kind() {
            return kind;
        }
    }
}
