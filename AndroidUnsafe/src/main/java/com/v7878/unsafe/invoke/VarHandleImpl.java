package com.v7878.unsafe.invoke;

import com.v7878.foreign.VarHandle;
import com.v7878.unsafe.invoke.Transformers.TransformerI;

import java.lang.invoke.MethodHandle;
import java.util.EnumSet;
import java.util.Objects;

public class VarHandleImpl extends AbstractVarHandle {

    public final boolean isAccessModeSupported(AccessMode accessMode) {
        int testBit = 1 << accessMode.ordinal();
        return (accessModesBitMask & testBit) == testBit;
    }

    @FunctionalInterface
    public interface VarHandleTransformer {
        void transform(VarHandleImpl handle, AccessMode mode, EmulatedStackFrame stack);
    }

    public static VarHandle newVarHandle(int accessModesBitMask, VarHandleTransformer impl,
                                         Class<?> varType, Class<?>... coordinates) {
        if ((accessModesBitMask & ~ALL_MODES_BIT_MASK) != 0) {
            throw new IllegalArgumentException("illegal accessModesBitMask: " + accessModesBitMask);
        }
        Objects.requireNonNull(impl);
        checkVarType(varType);
        checkCoordinates(coordinates);
        return new VarHandleImpl(accessModesBitMask, impl, varType, coordinates);
    }

    /**
     * BitMask of supported access mode indexed by AccessMode.ordinal().
     */
    private final int accessModesBitMask;

    private final VarHandleTransformer impl;

    private VarHandleImpl(int accessModesBitMask, VarHandleTransformer impl,
                          Class<?> varType, Class<?>... coordinates) {
        super(varType, coordinates);
        this.accessModesBitMask = accessModesBitMask;
        this.impl = impl;
    }

    protected MethodHandle getMethodHandleUncached(int modeOrdinal) {
        AccessMode mode = AccessMode.values()[modeOrdinal];
        return Transformers.makeTransformer(accessModeType(mode),
                (TransformerI) stack -> impl.transform(this, mode, stack));
    }

    /**
     * BitMask of access modes that do not change the memory referenced by a VarHandle.
     * An example being a read of a variable with volatile ordering effects.
     */
    public final static int READ_ACCESS_MODES_BIT_MASK;
    /**
     * BitMask of access modes that write to the memory referenced by
     * a VarHandle.  This does not include any compare and update
     * access modes, nor any bitwise or numeric access modes. An
     * example being a write to variable with release ordering
     * effects.
     */
    public final static int WRITE_ACCESS_MODES_BIT_MASK;
    /**
     * BitMask of access modes that are applicable to types
     * supporting for atomic updates.  This includes access modes that
     * both read and write a variable such as compare-and-set.
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
        WRITE_ACCESS_MODES_BIT_MASK = accessTypesToBitMask(EnumSet.of(AccessType.SET));
        ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK =
                accessTypesToBitMask(EnumSet.of(AccessType.COMPARE_AND_EXCHANGE,
                        AccessType.COMPARE_AND_SET, AccessType.GET_AND_UPDATE));
        NUMERIC_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK =
                accessTypesToBitMask(EnumSet.of(AccessType.GET_AND_UPDATE_NUMERIC));
        BITWISE_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK =
                accessTypesToBitMask(EnumSet.of(AccessType.GET_AND_UPDATE_BITWISE));
        ALL_MODES_BIT_MASK = (READ_ACCESS_MODES_BIT_MASK | WRITE_ACCESS_MODES_BIT_MASK |
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

    public static boolean isReadOnly(AccessMode accessMode) {
        return accessType(accessMode) == AccessType.GET;
    }
}
