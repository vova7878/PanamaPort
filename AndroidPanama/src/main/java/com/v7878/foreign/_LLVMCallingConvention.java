package com.v7878.foreign;

import static com.v7878.foreign.MemoryLayout.sequenceLayout;
import static com.v7878.foreign.MemoryLayout.structLayout;
import static com.v7878.foreign.ValueLayout.JAVA_BYTE;
import static com.v7878.foreign.ValueLayout.JAVA_DOUBLE;
import static com.v7878.foreign.ValueLayout.JAVA_FLOAT;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.foreign.ValueLayout.JAVA_LONG;
import static com.v7878.foreign.ValueLayout.JAVA_SHORT;
import static com.v7878.misc.Math.roundUp;
import static com.v7878.unsafe.InstructionSet.ARM64;
import static com.v7878.unsafe.InstructionSet.CURRENT_INSTRUCTION_SET;
import static com.v7878.unsafe.InstructionSet.X86;
import static com.v7878.unsafe.InstructionSet.X86_64;
import static com.v7878.unsafe.Utils.shouldNotReachHere;

import android.util.Pair;

import com.v7878.foreign._StorageDescriptor.LLVMStorage;
import com.v7878.foreign._StorageDescriptor.MemoryStorage;
import com.v7878.foreign._StorageDescriptor.NoStorage;
import com.v7878.foreign._StorageDescriptor.RawStorage;
import com.v7878.foreign._StorageDescriptor.WrapperStorage;

import java.util.Arrays;

final class _LLVMCallingConvention {
    private static class x86_android {
        public static _StorageDescriptor computeStorages(FunctionDescriptor descriptor) {
            LLVMStorage retStorage = descriptor.returnLayout()
                    .map(layout -> layout instanceof GroupLayout ?
                            new MemoryStorage(layout) : new RawStorage(layout))
                    .orElse(new NoStorage(null));
            LLVMStorage[] argStorages = descriptor.argumentLayouts().stream()
                    .map(layout -> layout instanceof GroupLayout ?
                            new MemoryStorage(layout) : new RawStorage(layout))
                    .toArray(LLVMStorage[]::new);
            return new _StorageDescriptor(retStorage, argStorages);
        }
    }

    private static class x86_64_android {

        private enum WrapperType {
            FP, INT
        }

        private static void markWrapperType(WrapperType[] types, int start, int size, WrapperType type) {
            for (int i = 0; i < size; i++) {
                WrapperType oldType = types[start + i];
                if (oldType == null || oldType.ordinal() < type.ordinal()) {
                    types[start + i] = type;
                }
            }
        }

        private static void markWrapperType(MemoryLayout layout, int offset, WrapperType[] types) {
            if (layout instanceof StructLayout sl) {
                for (MemoryLayout member : sl.memberLayouts()) {
                    markWrapperType(member, offset, types);
                    offset = Math.addExact(offset, Math.toIntExact(member.byteSize()));
                }
            } else if (layout instanceof UnionLayout ul) {
                for (MemoryLayout member : ul.memberLayouts()) {
                    markWrapperType(member, offset, types);
                }
            } else if (layout instanceof SequenceLayout sl) {
                MemoryLayout el = sl.elementLayout();
                int count = Math.toIntExact(sl.elementCount());
                int size = Math.toIntExact(el.byteSize());
                for (int i = 0; i < count; i++) {
                    markWrapperType(el, offset, types);
                    offset = Math.addExact(offset, size);
                }
            } else if (layout instanceof ValueLayout vl) {
                WrapperType type;
                if (layout instanceof ValueLayout.OfFloat || layout instanceof ValueLayout.OfDouble) {
                    type = WrapperType.FP;
                } else {
                    type = WrapperType.INT;
                }
                markWrapperType(types, offset, Math.toIntExact(vl.byteSize()), type);
            } else if (layout instanceof PaddingLayout) {
                // skip
            } else {
                throw shouldNotReachHere();
            }
        }

        private static ValueLayout getWrapper(WrapperType[] types, int count) {
            WrapperType max = null;
            for (WrapperType type : types) {
                if (max == null) {
                    max = type;
                }
                if (type != null && type.ordinal() > max.ordinal()) {
                    max = type;
                }
            }
            assert max != null;
            assert max == WrapperType.INT || (count == 4 || count == 8);
            return switch (count) {
                case 0 -> throw shouldNotReachHere();
                case 1 -> JAVA_BYTE;
                case 2 -> JAVA_SHORT;
                case 3 -> JAVA_INT;
                case 4 -> max == WrapperType.INT ? JAVA_INT : JAVA_FLOAT;
                case 8 -> max == WrapperType.INT ? JAVA_LONG : JAVA_DOUBLE;
                default -> JAVA_LONG;
            };
        }

        public static ValueLayout[] getWrappers(GroupLayout layout) {
            if (layout.byteSize() > 16 /* 128 bit */) return null;
            WrapperType[] types = new WrapperType[16];
            markWrapperType(layout, 0, types);
            int count = 16;
            while (count > 0 && types[count - 1] == null) {
                count--;
            }
            WrapperType[] upper_half = Arrays.copyOf(types, 8);
            if (count <= 8) {
                return new ValueLayout[]{getWrapper(upper_half, count)};
            }
            WrapperType[] lower_half = Arrays.copyOfRange(types, 8, 16);
            return new ValueLayout[]{getWrapper(upper_half, 8),
                    getWrapper(lower_half, count - 8)};
        }

        public static boolean isFP(ValueLayout layout) {
            return layout instanceof ValueLayout.OfDouble
                    || layout instanceof ValueLayout.OfFloat;
        }

        public static _StorageDescriptor computeStorages(FunctionDescriptor descriptor) {
            final int[] arg_regs = {/* integer regs */ 6, /* floating point regs */ 8};
            LLVMStorage retStorage = descriptor.returnLayout().map(layout -> {
                if (layout instanceof ValueLayout vl) {
                    return new RawStorage(vl);
                }
                if (layout instanceof GroupLayout gl) {
                    ValueLayout[] tmp = getWrappers(gl);
                    if (tmp == null) {
                        arg_regs[0] = Math.max(0, arg_regs[0] - 1); // pointer arg
                        return new MemoryStorage(gl);
                    }
                    MemoryLayout wrapper = tmp.length == 1 ? tmp[0] : structLayout(tmp[0], tmp[1]);
                    return new WrapperStorage(gl, wrapper);
                }
                throw shouldNotReachHere();
            }).orElse(new NoStorage(null));
            LLVMStorage[] argStorages = descriptor.argumentLayouts().stream().map(layout -> {
                if (layout instanceof ValueLayout vl) {
                    int type = isFP(vl) ? 1 : 0;
                    arg_regs[type] = Math.max(0, arg_regs[type] - 1);
                    return new RawStorage(vl);
                }
                if (layout instanceof GroupLayout gl) {
                    ValueLayout[] tmp = getWrappers(gl);
                    if (tmp != null) {
                        if (tmp.length == 1) {
                            int type = isFP(tmp[0]) ? 1 : 0;
                            if (arg_regs[type] >= 1) {
                                arg_regs[type]--;
                                return new WrapperStorage(gl, tmp[0]);
                            }
                        } else {
                            MemoryLayout wrapper = structLayout(tmp[0], tmp[1]);
                            if (tmp[0] == tmp[1]) {
                                int type = isFP(tmp[0]) ? 1 : 0;
                                if (arg_regs[type] >= 2) {
                                    arg_regs[type] -= 2;
                                    return new WrapperStorage(gl, wrapper);
                                }
                            } else {
                                if (arg_regs[0] >= 1 && arg_regs[1] >= 1) {
                                    arg_regs[0]--;
                                    arg_regs[1]--;
                                    return new WrapperStorage(gl, wrapper);
                                }
                            }
                        }
                    }
                    arg_regs[0] = Math.max(0, arg_regs[0] - 1); // pointer arg
                    return new MemoryStorage(gl);
                }
                throw shouldNotReachHere();
            }).toArray(LLVMStorage[]::new);
            return new _StorageDescriptor(retStorage, argStorages);
        }
    }

    private static class aarch64_android {

        private enum WrapperType {
            FLOAT, DOUBLE, INT
        }

        private static void markWrapperType(WrapperType[] types, int start, int size, WrapperType type) {
            for (int i = 0; i < size; i++) {
                WrapperType oldType = types[start + i];
                if (oldType == null) {
                    types[start + i] = type;
                } else if (oldType != type) {
                    types[start + i] = WrapperType.INT;
                }
            }
        }

        private static void markWrapperType(MemoryLayout layout, int offset, WrapperType[] types) {
            if (layout instanceof StructLayout sl) {
                for (MemoryLayout member : sl.memberLayouts()) {
                    markWrapperType(member, offset, types);
                    offset = Math.addExact(offset, Math.toIntExact(member.byteSize()));
                }
            } else if (layout instanceof UnionLayout ul) {
                for (MemoryLayout member : ul.memberLayouts()) {
                    markWrapperType(member, offset, types);
                }
            } else if (layout instanceof SequenceLayout sl) {
                MemoryLayout el = sl.elementLayout();
                int count = Math.toIntExact(sl.elementCount());
                int size = Math.toIntExact(el.byteSize());
                for (int i = 0; i < count; i++) {
                    markWrapperType(el, offset, types);
                    offset = Math.addExact(offset, size);
                }
            } else if (layout instanceof ValueLayout vl) {
                WrapperType type;
                if (layout instanceof ValueLayout.OfFloat) {
                    type = WrapperType.FLOAT;
                } else if (layout instanceof ValueLayout.OfDouble) {
                    type = WrapperType.DOUBLE;
                } else {
                    type = WrapperType.INT;
                }
                markWrapperType(types, offset, Math.toIntExact(vl.byteSize()), type);
            } else if (layout instanceof PaddingLayout) {
                // skip
            } else {
                throw shouldNotReachHere();
            }
        }

        public static Pair<ValueLayout, Integer> getWrappers(GroupLayout layout) {
            if (layout.byteSize() > 32 /* 256 bit */) return null;
            WrapperType[] types = new WrapperType[32];
            markWrapperType(layout, 0, types);
            int count = 32;
            while (count > 0 && types[count - 1] == null) {
                count--;
            }
            types = Arrays.copyOfRange(types, 0, count);
            WrapperType common = null;
            for (WrapperType type : types) {
                if (common == null) {
                    common = type;
                }
                if (common != type) {
                    common = WrapperType.INT;
                    break;
                }
            }
            common = common == null ? WrapperType.INT : common;
            if (count > 16 /* 128 bit */ && common != WrapperType.DOUBLE) {
                return null;
            }
            int element_size = common == WrapperType.FLOAT ? 4 : 8;
            int element_count = roundUp(count, element_size) / (element_size);
            return switch (common) {
                case INT -> new Pair<>(JAVA_LONG, element_count);
                case DOUBLE -> new Pair<>(JAVA_DOUBLE, element_count);
                case FLOAT -> new Pair<>(JAVA_FLOAT, element_count);
                //noinspection UnnecessaryDefault
                default -> throw shouldNotReachHere();
            };
        }

        public static _StorageDescriptor computeStorages(FunctionDescriptor descriptor) {
            LLVMStorage retStorage = descriptor.returnLayout().map(layout -> {
                if (layout instanceof ValueLayout vl) {
                    return new RawStorage(vl);
                }
                if (layout instanceof GroupLayout gl) {
                    var info = getWrappers(gl);
                    if (info == null) {
                        return new MemoryStorage(gl);
                    }
                    MemoryLayout wrapper = sequenceLayout(info.second, info.first);
                    return new WrapperStorage(gl, wrapper);
                }
                throw shouldNotReachHere();
            }).orElse(new NoStorage(null));
            LLVMStorage[] argStorages = descriptor.argumentLayouts().stream().map(layout -> {
                if (layout instanceof ValueLayout vl) {
                    return new RawStorage(vl);
                }
                if (layout instanceof GroupLayout gl) {
                    var info = getWrappers(gl);
                    if (info == null) {
                        return new MemoryStorage(gl);
                    }
                    MemoryLayout wrapper = sequenceLayout(info.second, info.first);
                    return new WrapperStorage(gl, wrapper);
                }
                throw shouldNotReachHere();
            }).toArray(LLVMStorage[]::new);
            return new _StorageDescriptor(retStorage, argStorages);
        }
    }

    public static _StorageDescriptor computeStorages(FunctionDescriptor descriptor) {
        if (CURRENT_INSTRUCTION_SET == X86) return x86_android.computeStorages(descriptor);
        if (CURRENT_INSTRUCTION_SET == X86_64) return x86_64_android.computeStorages(descriptor);
        if (CURRENT_INSTRUCTION_SET == ARM64) return aarch64_android.computeStorages(descriptor);
        //TODO: arm, riscv64
        throw new UnsupportedOperationException("Not supported yet!");
    }
}
