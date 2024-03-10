package com.v7878.foreign;

import static com.v7878.unsafe.InstructionSet.CURRENT_INSTRUCTION_SET;
import static com.v7878.unsafe.InstructionSet.X86;

import com.v7878.foreign._StorageDescriptor.LLVMStorage;
import com.v7878.foreign._StorageDescriptor.NoStorage;
import com.v7878.foreign._StorageDescriptor.RawStorage;
import com.v7878.foreign._StorageDescriptor.StackStorage;

final class _LLVMCallingConvention {
    private static class x86 {
        public static _StorageDescriptor computeStorages(FunctionDescriptor descriptor) {
            LLVMStorage retStorage = descriptor.returnLayout()
                    .map(layout -> layout instanceof GroupLayout ?
                            new StackStorage(layout) : new RawStorage(layout))
                    .orElse(new NoStorage(null));
            LLVMStorage[] argStorages = descriptor.argumentLayouts().stream()
                    .map(layout -> layout instanceof GroupLayout ?
                            new StackStorage(layout) : new RawStorage(layout))
                    .toArray(LLVMStorage[]::new);
            return new _StorageDescriptor(retStorage, argStorages);
        }
    }

    //private static class x86_64_calling_convention {
    //
    //    private static void markWrapperType(_AndroidLinkerImpl.WrapperType[] types, int start, int size, _AndroidLinkerImpl.WrapperType type) {
    //        for (int i = 0; i < size; i++) {
    //            _AndroidLinkerImpl.WrapperType oldType = types[start + i];
    //            if (oldType == null || oldType.ordinal() < type.ordinal()) {
    //                types[start + i] = type;
    //            }
    //        }
    //    }
    //
    //    private static void markWrapperType(MemoryLayout layout, int offset, _AndroidLinkerImpl.WrapperType[] types) {
    //        if (layout instanceof StructLayout sl) {
    //            for (MemoryLayout member : sl.memberLayouts()) {
    //                markWrapperType(member, offset, types);
    //                offset = Math.addExact(offset, Math.toIntExact(member.byteSize()));
    //            }
    //        } else if (layout instanceof UnionLayout ul) {
    //            for (MemoryLayout member : ul.memberLayouts()) {
    //                markWrapperType(member, offset, types);
    //            }
    //        } else if (layout instanceof SequenceLayout sl) {
    //            MemoryLayout el = sl.elementLayout();
    //            int count = Math.toIntExact(sl.elementCount());
    //            int size = Math.toIntExact(el.byteSize());
    //            for (int i = 0; i < count; i++) {
    //                markWrapperType(el, offset, types);
    //                offset = Math.addExact(offset, size);
    //            }
    //        } else if (layout instanceof ValueLayout vl) {
    //            _AndroidLinkerImpl.WrapperType type;
    //            if (layout instanceof ValueLayout.OfFloat) {
    //                type = _AndroidLinkerImpl.WrapperType.FLOAT;
    //            } else if (layout instanceof ValueLayout.OfDouble) {
    //                type = _AndroidLinkerImpl.WrapperType.DOUBLE;
    //            } else {
    //                type = _AndroidLinkerImpl.WrapperType.INT;
    //            }
    //            markWrapperType(types, offset, Math.toIntExact(vl.byteSize()), type);
    //        } else if (layout instanceof PaddingLayout) {
    //            // skip
    //        } else {
    //            throw shouldNotReachHere();
    //        }
    //    }
    //
    //    private static MemoryLayout getGroupWrapper(_AndroidLinkerImpl.WrapperType[] types, int count) {
    //        _AndroidLinkerImpl.WrapperType max = null;
    //        for (_AndroidLinkerImpl.WrapperType type : types) {
    //            if (max == null) {
    //                max = type;
    //            }
    //            if (type != null && type.ordinal() > max.ordinal()) {
    //                max = type;
    //            }
    //        }
    //        return switch (count) {
    //            case 0 -> VOID_WRAPPER;
    //            case 1 -> JAVA_BYTE;
    //            case 2 -> JAVA_SHORT;
    //            case 3 -> JAVA_INT;
    //            case 4 -> max == _AndroidLinkerImpl.WrapperType.INT ? JAVA_INT : JAVA_FLOAT;
    //            case 8 ->
    //                    max == _AndroidLinkerImpl.WrapperType.INT ? JAVA_LONG : (max == _AndroidLinkerImpl.WrapperType.DOUBLE ?
    //                            JAVA_DOUBLE : structLayout(JAVA_FLOAT, JAVA_FLOAT));
    //            default -> JAVA_LONG;
    //        };
    //    }
    //
    //    public static MemoryLayout getGroupWrapper(GroupLayout layout) {
    //        if (layout.byteSize() > 16 /* 128 bit */) return null;
    //        _AndroidLinkerImpl.WrapperType[] types = new _AndroidLinkerImpl.WrapperType[16];
    //        markWrapperType(layout, 0, types);
    //        int count = 16;
    //        while (count > 0 && types[count - 1] == null) {
    //            count--;
    //        }
    //        _AndroidLinkerImpl.WrapperType[] upper_half = Arrays.copyOf(types, 8);
    //        if (count <= 8) {
    //            return getGroupWrapper(upper_half, count);
    //        }
    //        _AndroidLinkerImpl.WrapperType[] lower_half = Arrays.copyOfRange(types, 8, 16);
    //        return structLayout(getGroupWrapper(upper_half, 8),
    //                getGroupWrapper(lower_half, count - 8));
    //    }
    //}
    //
    //private static class aarch64_calling_convention {
    //    private static void markWrapperType(_AndroidLinkerImpl.WrapperType[] types, int start, int size, _AndroidLinkerImpl.WrapperType type) {
    //        for (int i = 0; i < size; i++) {
    //            _AndroidLinkerImpl.WrapperType oldType = types[start + i];
    //            if (oldType == null) {
    //                types[start + i] = type;
    //            } else if (oldType.ordinal() != type.ordinal()) {
    //                types[start + i] = _AndroidLinkerImpl.WrapperType.INT;
    //            }
    //        }
    //    }
    //
    //    private static void markWrapperType(MemoryLayout layout, int offset, _AndroidLinkerImpl.WrapperType[] types) {
    //        if (layout instanceof StructLayout sl) {
    //            for (MemoryLayout member : sl.memberLayouts()) {
    //                markWrapperType(member, offset, types);
    //                offset = Math.addExact(offset, Math.toIntExact(member.byteSize()));
    //            }
    //        } else if (layout instanceof UnionLayout ul) {
    //            for (MemoryLayout member : ul.memberLayouts()) {
    //                markWrapperType(member, offset, types);
    //            }
    //        } else if (layout instanceof SequenceLayout sl) {
    //            MemoryLayout el = sl.elementLayout();
    //            int count = Math.toIntExact(sl.elementCount());
    //            int size = Math.toIntExact(el.byteSize());
    //            for (int i = 0; i < count; i++) {
    //                markWrapperType(el, offset, types);
    //                offset = Math.addExact(offset, size);
    //            }
    //        } else if (layout instanceof ValueLayout vl) {
    //            _AndroidLinkerImpl.WrapperType type;
    //            if (layout instanceof ValueLayout.OfFloat) {
    //                type = _AndroidLinkerImpl.WrapperType.FLOAT;
    //            } else if (layout instanceof ValueLayout.OfDouble) {
    //                type = _AndroidLinkerImpl.WrapperType.DOUBLE;
    //            } else {
    //                type = _AndroidLinkerImpl.WrapperType.INT;
    //            }
    //            markWrapperType(types, offset, Math.toIntExact(vl.byteSize()), type);
    //        } else if (layout instanceof PaddingLayout) {
    //            // skip
    //        } else {
    //            throw shouldNotReachHere();
    //        }
    //    }
    //
    //    public static MemoryLayout getGroupWrapper(GroupLayout layout) {
    //        if (layout.byteSize() > 32 /* 256 bit */) return null;
    //        _AndroidLinkerImpl.WrapperType[] types = new _AndroidLinkerImpl.WrapperType[32];
    //        markWrapperType(layout, 0, types);
    //        int count = 32;
    //        while (count > 0 && types[count - 1] == null) {
    //            count--;
    //        }
    //        types = Arrays.copyOfRange(types, 0, count);
    //        _AndroidLinkerImpl.WrapperType common = null;
    //        for (_AndroidLinkerImpl.WrapperType type : types) {
    //            if (common == null) {
    //                common = type;
    //            }
    //            if (common != type) {
    //                common = _AndroidLinkerImpl.WrapperType.INT;
    //                break;
    //            }
    //        }
    //        common = common == null ? _AndroidLinkerImpl.WrapperType.INT : common;
    //        if (count > 16 /* 128 bit */ && common != _AndroidLinkerImpl.WrapperType.DOUBLE) {
    //            return null;
    //        }
    //        return switch (common) {
    //            case INT -> count <= 8 ? JAVA_LONG : structLayout(JAVA_LONG, JAVA_LONG);
    //            case DOUBLE -> sequenceLayout(count / 8, JAVA_DOUBLE);
    //            case FLOAT -> sequenceLayout(count / 4, JAVA_FLOAT);
    //            //noinspection UnnecessaryDefault
    //            default -> throw shouldNotReachHere();
    //        };
    //    }
    //}
    //
    //private enum WrapperType {
    //    FLOAT, DOUBLE, INT
    //}

    public static _StorageDescriptor computeStorages(FunctionDescriptor descriptor) {
        if (CURRENT_INSTRUCTION_SET == X86) return x86.computeStorages(descriptor);
        //TODO: x86_64, aarch64, arm, riscv64
        throw new UnsupportedOperationException("Not supported yet!");
    }
}
