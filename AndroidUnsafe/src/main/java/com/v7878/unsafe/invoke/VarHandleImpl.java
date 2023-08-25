package com.v7878.unsafe.invoke;

import static com.v7878.unsafe.Utils.nothrows_run;

import com.v7878.foreign.VarHandle;
import com.v7878.unsafe.invoke.Transformers.TransformerI;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class VarHandleImpl extends VarHandle {
    public final Object get(Object... args) {
        return nothrows_run(() -> toMethodHandle(AccessMode.GET).invokeWithArguments(args));
    }

    public final void set(Object... args) {
        nothrows_run(() -> toMethodHandle(AccessMode.SET).invokeWithArguments(args));
    }

    // Volatile accessors

    public final Object getVolatile(Object... args) {
        return nothrows_run(() -> toMethodHandle(AccessMode.GET_VOLATILE).invokeWithArguments(args));
    }

    public final void setVolatile(Object... args) {
        nothrows_run(() -> toMethodHandle(AccessMode.SET_VOLATILE).invokeWithArguments(args));
    }

    public final Object getOpaque(Object... args) {
        return nothrows_run(() -> toMethodHandle(AccessMode.GET_OPAQUE).invokeWithArguments(args));
    }

    public final void setOpaque(Object... args) {
        nothrows_run(() -> toMethodHandle(AccessMode.SET_OPAQUE).invokeWithArguments(args));
    }

    // Lazy accessors

    public final Object getAcquire(Object... args) {
        return nothrows_run(() -> toMethodHandle(AccessMode.GET_ACQUIRE).invokeWithArguments(args));
    }

    public final void setRelease(Object... args) {
        nothrows_run(() -> toMethodHandle(AccessMode.SET_RELEASE).invokeWithArguments(args));
    }

    // Compare and set accessors

    public final boolean compareAndSet(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object compareAndExchange(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object compareAndExchangeAcquire(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object compareAndExchangeRelease(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    // Weak (spurious failures allowed)

    public final boolean weakCompareAndSetPlain(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final boolean weakCompareAndSet(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final boolean weakCompareAndSetAcquire(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final boolean weakCompareAndSetRelease(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndSet(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndSetAcquire(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndSetRelease(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    // Primitive adders
    // Throw UnsupportedOperationException for refs

    public final Object getAndAdd(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndAddAcquire(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndAddRelease(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    // Bitwise operations
    // Throw UnsupportedOperationException for refs

    public final Object getAndBitwiseOr(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndBitwiseOrAcquire(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndBitwiseOrRelease(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndBitwiseAnd(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndBitwiseAndAcquire(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndBitwiseAndRelease(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndBitwiseXor(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndBitwiseXorAcquire(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndBitwiseXorRelease(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    //

    public final Class<?> varType() {
        return varType;
    }

    public final List<Class<?>> coordinateTypes() {
        //TODO: improve
        MethodType typeGet = accessModeType(AccessMode.GET);
        return typeGet.parameterList();
    }

    public final MethodType accessModeType(AccessMode accessMode) {
        return accessModeType(accessType(accessMode).ordinal());
    }

    public final boolean isAccessModeSupported(AccessMode accessMode) {
        int testBit = 1 << accessMode.ordinal();
        return (accessModesBitMask & testBit) == testBit;
    }

    public final MethodHandle toMethodHandle(AccessMode accessMode) {
        if (isAccessModeSupported(accessMode)) {
            return getMethodHandle(accessMode.ordinal());
        } else {
            //TODO
            throw new UnsupportedOperationException("Not supported yet");
        }
    }

    // implementation details

    @FunctionalInterface
    public interface VarHandleTransformer {
        void transform(VarHandleImpl handle, AccessMode mode, EmulatedStackFrame stack);
    }

    private static Class<?> requireNonVoid(Class<?> clazz) {
        if (clazz == void.class) {
            throw new IllegalArgumentException("parameter type cannot be void");
        }
        return clazz;
    }

    public static VarHandle newVarHandle(
            int accessModesBitMask, VarHandleTransformer impl, Class<?> varType,
            Class<?> receiver, Class<?>... intermediates) {
        if ((accessModesBitMask & ~ALL_MODES_BIT_MASK) != 0) {
            throw new IllegalArgumentException("illegal accessModesBitMask: " + accessModesBitMask);
        }
        Objects.requireNonNull(impl);
        Objects.requireNonNull(requireNonVoid(varType));
        requireNonVoid(receiver);
        Stream.of(Objects.requireNonNull(intermediates))
                .map(Objects::requireNonNull)
                .forEach(VarHandleImpl::requireNonVoid);
        return new VarHandleImpl(accessModesBitMask, impl, varType, receiver, intermediates);
    }

    /**
     * The target type for accesses.
     */
    private final Class<?> varType;
    /**
     * The receiver of this VarHandle, or null if this VarHandle has no receiver.
     */
    private final Class<?> receiver;
    /**
     * Intermediate coordinates of this VarHandle.
     */
    private final Class<?>[] intermediates;
    /**
     * BitMask of supported access mode indexed by AccessMode.ordinal().
     */
    private final int accessModesBitMask;

    private final VarHandleTransformer impl;

    private VarHandleImpl(int accessModesBitMask, VarHandleTransformer impl, Class<?> varType,
                          Class<?> receiver, Class<?>... intermediates) {
        this.accessModesBitMask = accessModesBitMask;
        this.impl = impl;
        this.varType = varType;
        this.receiver = receiver;
        this.intermediates = intermediates;
    }

    private MethodType[] methodTypeTable;
    private MethodHandle[] methodHandleTable;

    private MethodType accessModeType(int type) {
        MethodType[] mtTable = methodTypeTable;
        if (mtTable == null) {
            mtTable = methodTypeTable = new MethodType[AccessType.values().length];
        }
        MethodType mt = mtTable[type];
        if (mt == null) {
            mt = mtTable[type] = accessModeTypeUncached(type);
        }
        return mt;
    }

    private MethodType accessModeTypeUncached(int type) {
        return AccessType.values()[type].accessModeType(receiver, varType, intermediates);
    }

    private MethodHandle getMethodHandle(int mode) {
        MethodHandle[] mhTable = methodHandleTable;
        if (mhTable == null) {
            mhTable = methodHandleTable = new MethodHandle[AccessMode.values().length];
        }
        MethodHandle mh = mhTable[mode];
        if (mh == null) {
            mh = mhTable[mode] = getMethodHandleUncached(mode);
        }
        return mh;
    }

    private MethodHandle getMethodHandleUncached(int modeOrdinal) {
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

    private static int accessTypesToBitMask(final EnumSet<AccessType> accessTypes) {
        int m = 0;
        for (AccessMode accessMode : AccessMode.values()) {
            if (accessTypes.contains(accessType(accessMode))) {
                m |= 1 << accessMode.ordinal();
            }
        }
        return m;
    }
}
