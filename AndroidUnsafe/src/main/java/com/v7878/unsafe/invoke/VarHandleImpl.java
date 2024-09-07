package com.v7878.unsafe.invoke;

import com.v7878.invoke.VarHandle;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.EnumSet;
import java.util.Objects;

public class VarHandleImpl extends AbstractVarHandle {
    @FunctionalInterface
    public interface VarHandleTransformer {
        void transform(VarHandleImpl handle, AccessMode mode, EmulatedStackFrame stack);
    }

    @FunctionalInterface
    public interface VarHandleFactory {
        MethodHandle create(VarHandleImpl handle, AccessMode mode, MethodType type);
    }

    public static VarHandle newVarHandle(int accessModesBitMask, VarHandleTransformer impl,
                                         Class<?> varType, Class<?>... coordinates) {
        Objects.requireNonNull(impl);
        return newVarHandle(accessModesBitMask, (thiz, mode, type) -> {
            return Transformers.makeTransformer(type,
                    (ignored, stack) -> impl.transform(thiz, mode, stack));
        }, varType, coordinates);
    }

    public static VarHandle newVarHandle(int accessModesBitMask, VarHandleFactory handleFactory,
                                         Class<?> varType, Class<?>... coordinates) {
        if ((accessModesBitMask & ~ALL_MODES_BIT_MASK) != 0) {
            throw new IllegalArgumentException("illegal accessModesBitMask: " + accessModesBitMask);
        }
        Objects.requireNonNull(handleFactory);
        checkVarType(varType);
        checkCoordinates(coordinates);
        return new VarHandleImpl(accessModesBitMask, handleFactory, varType, coordinates);
    }

    /**
     * BitMask of supported access mode indexed by AccessMode.ordinal().
     */
    private final int accessModesBitMask;

    private final VarHandleFactory handleFactory;

    private VarHandleImpl(int accessModesBitMask, VarHandleFactory handleFactory,
                          Class<?> varType, Class<?>... coordinates) {
        super(varType, coordinates);
        this.accessModesBitMask = accessModesBitMask;
        this.handleFactory = handleFactory;
    }

    @Override
    public final boolean isAccessModeSupported(AccessMode accessMode) {
        int testBit = 1 << accessMode.ordinal();
        return (accessModesBitMask & testBit) == testBit;
    }

    @Override
    protected MethodHandle getMethodHandleUncached(AccessMode mode) {
        MethodType type = accessModeType(mode);
        MethodHandle out = handleFactory.create(this, mode, type);
        if (!out.type().equals(type)) { // NPE check
            throw new IllegalStateException(
                    "handleFactory returned MethodHandle with wrong type: " + out.type());
        }
        return out;
    }

    /**
     * BitMask of access modes that do not change the memory referenced by a VarHandle.
     */
    public final static int READ_ACCESS_MODES_BIT_MASK;
    public final static int READ_ATOMIC_ACCESS_MODES_BIT_MASK;
    /**
     * BitMask of access modes that write to the memory referenced by
     * a VarHandle. This does not include any compare and update
     * access modes, nor any bitwise or numeric access modes.
     */
    public final static int WRITE_ACCESS_MODES_BIT_MASK;
    public final static int WRITE_ATOMIC_ACCESS_MODES_BIT_MASK;
    /**
     * BitMask of access modes that are applicable to types
     * supporting for atomic updates.
     */
    public final static int ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK;
    /**
     * BitMask of access modes that are applicable to types
     * supporting numeric atomic update operations.
     */
    public final static int NUMERIC_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK;
    /**
     * BitMask of access modes that are applicable to types
     * supporting bitwise atomic update operations.
     */
    public final static int BITWISE_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK;
    /**
     * BitMask of all access modes.
     */
    public final static int ALL_MODES_BIT_MASK;

    static {
        // Check we're not about to overflow the storage of the
        // bitmasks here and in the accessModesBitMask field.
        if (AccessMode.values().length > Integer.SIZE) {
            throw new InternalError("accessModes overflow");
        }

        READ_ACCESS_MODES_BIT_MASK = accessTypesToBitMask(EnumSet.of(AccessType.GET));
        READ_ATOMIC_ACCESS_MODES_BIT_MASK = accessTypesToBitMask(EnumSet.of(AccessType.GET_ATOMIC));
        WRITE_ACCESS_MODES_BIT_MASK = accessTypesToBitMask(EnumSet.of(AccessType.SET));
        WRITE_ATOMIC_ACCESS_MODES_BIT_MASK = accessTypesToBitMask(EnumSet.of(AccessType.SET_ATOMIC));
        ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK =
                accessTypesToBitMask(EnumSet.of(AccessType.COMPARE_AND_EXCHANGE,
                        AccessType.COMPARE_AND_SET, AccessType.GET_AND_UPDATE));
        NUMERIC_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK =
                accessTypesToBitMask(EnumSet.of(AccessType.GET_AND_UPDATE_NUMERIC));
        BITWISE_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK =
                accessTypesToBitMask(EnumSet.of(AccessType.GET_AND_UPDATE_BITWISE));

        ALL_MODES_BIT_MASK = (READ_ACCESS_MODES_BIT_MASK | READ_ATOMIC_ACCESS_MODES_BIT_MASK |
                WRITE_ACCESS_MODES_BIT_MASK | WRITE_ATOMIC_ACCESS_MODES_BIT_MASK |
                ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK |
                NUMERIC_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK |
                BITWISE_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK);
    }

    public static int accessTypesToBitMask(final EnumSet<AccessType> accessTypes) {
        int m = 0;
        for (AccessMode accessMode : AccessMode.values()) {
            if (accessTypes.contains(accessType(accessMode))) {
                m |= 1 << accessMode.ordinal();
            }
        }
        return m;
    }

    public static int accessModesBitMask(Class<?> varType, boolean allowAtomicAccess) {
        int bitMask = ALL_MODES_BIT_MASK;
        if (!allowAtomicAccess) {
            bitMask &= ~(READ_ATOMIC_ACCESS_MODES_BIT_MASK | WRITE_ATOMIC_ACCESS_MODES_BIT_MASK);
        }
        if (!allowAtomicAccess || (varType != byte.class && varType != short.class
                && varType != char.class && varType != int.class && varType != long.class
                && varType != float.class && varType != double.class)) {
            bitMask &= ~NUMERIC_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK;
        }
        if (!allowAtomicAccess || (varType != boolean.class && varType != byte.class
                && varType != short.class && varType != char.class
                && varType != int.class && varType != long.class)) {
            bitMask &= ~BITWISE_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK;
        }
        return bitMask;
    }

    public static boolean isReadOnly(AccessMode accessMode) {
        var type = accessType(accessMode);
        return type == AccessType.GET || type == AccessType.GET_ATOMIC;
    }
}
