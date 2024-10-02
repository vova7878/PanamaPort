package com.v7878.unsafe.invoke;

import static com.v7878.unsafe.Utils.nothrows_run;

import com.v7878.invoke.VarHandle;
import com.v7878.r8.annotations.AlwaysInline;
import com.v7878.unsafe.invoke.Transformers.AbstractTransformer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class VarHandleImpl extends VarHandle {
    @Override
    public final Object get(Object... args) {
        return invoke(AccessMode.GET, args);
    }

    @Override
    public final void set(Object... args) {
        invoke(AccessMode.SET, args);
    }

    // Volatile accessors

    @Override
    public final Object getVolatile(Object... args) {
        return invoke(AccessMode.GET_VOLATILE, args);
    }

    @Override
    public final void setVolatile(Object... args) {
        invoke(AccessMode.SET_VOLATILE, args);
    }

    @Override
    public final Object getOpaque(Object... args) {
        return invoke(AccessMode.GET_OPAQUE, args);
    }

    @Override
    public final void setOpaque(Object... args) {
        invoke(AccessMode.SET_OPAQUE, args);
    }

    // Lazy accessors

    @Override
    public final Object getAcquire(Object... args) {
        return invoke(AccessMode.GET_ACQUIRE, args);
    }

    @Override
    public final void setRelease(Object... args) {
        invoke(AccessMode.SET_RELEASE, args);
    }

    // Compare and set accessors

    @Override
    public final boolean compareAndSet(Object... args) {
        return (boolean) invoke(AccessMode.COMPARE_AND_SET, args);
    }

    @Override
    public final Object compareAndExchange(Object... args) {
        return invoke(AccessMode.COMPARE_AND_EXCHANGE, args);
    }

    @Override
    public final Object compareAndExchangeAcquire(Object... args) {
        return invoke(AccessMode.COMPARE_AND_EXCHANGE_ACQUIRE, args);
    }

    @Override
    public final Object compareAndExchangeRelease(Object... args) {
        return invoke(AccessMode.COMPARE_AND_EXCHANGE_RELEASE, args);
    }

    // Weak (spurious failures allowed)

    @Override
    public final boolean weakCompareAndSetPlain(Object... args) {
        return (boolean) invoke(AccessMode.WEAK_COMPARE_AND_SET_PLAIN, args);
    }

    @Override
    public final boolean weakCompareAndSet(Object... args) {
        return (boolean) invoke(AccessMode.WEAK_COMPARE_AND_SET, args);
    }

    @Override
    public final boolean weakCompareAndSetAcquire(Object... args) {
        return (boolean) invoke(AccessMode.WEAK_COMPARE_AND_SET_ACQUIRE, args);
    }

    @Override
    public final boolean weakCompareAndSetRelease(Object... args) {
        return (boolean) invoke(AccessMode.WEAK_COMPARE_AND_SET_RELEASE, args);
    }

    @Override
    public final Object getAndSet(Object... args) {
        return invoke(AccessMode.GET_AND_SET, args);
    }

    @Override
    public final Object getAndSetAcquire(Object... args) {
        return invoke(AccessMode.GET_AND_SET_ACQUIRE, args);
    }

    @Override
    public final Object getAndSetRelease(Object... args) {
        return invoke(AccessMode.GET_AND_SET_RELEASE, args);
    }

    // Primitive adders
    // Throw UnsupportedOperationException for refs

    @Override
    public final Object getAndAdd(Object... args) {
        return invoke(AccessMode.GET_AND_ADD, args);
    }

    @Override
    public final Object getAndAddAcquire(Object... args) {
        return invoke(AccessMode.GET_AND_ADD_ACQUIRE, args);
    }

    @Override
    public final Object getAndAddRelease(Object... args) {
        return invoke(AccessMode.GET_AND_ADD_RELEASE, args);
    }

    // Bitwise operations
    // Throw UnsupportedOperationException for refs

    @Override
    public final Object getAndBitwiseOr(Object... args) {
        return invoke(AccessMode.GET_AND_BITWISE_OR, args);
    }

    @Override
    public final Object getAndBitwiseOrAcquire(Object... args) {
        return invoke(AccessMode.GET_AND_BITWISE_OR_ACQUIRE, args);
    }

    @Override
    public final Object getAndBitwiseOrRelease(Object... args) {
        return invoke(AccessMode.GET_AND_BITWISE_OR_RELEASE, args);
    }

    @Override
    public final Object getAndBitwiseAnd(Object... args) {
        return invoke(AccessMode.GET_AND_BITWISE_AND, args);
    }

    @Override
    public final Object getAndBitwiseAndAcquire(Object... args) {
        return invoke(AccessMode.GET_AND_BITWISE_AND_ACQUIRE, args);
    }

    @Override
    public final Object getAndBitwiseAndRelease(Object... args) {
        return invoke(AccessMode.GET_AND_BITWISE_AND_RELEASE, args);
    }

    @Override
    public final Object getAndBitwiseXor(Object... args) {
        return invoke(AccessMode.GET_AND_BITWISE_XOR, args);
    }

    @Override
    public final Object getAndBitwiseXorAcquire(Object... args) {
        return invoke(AccessMode.GET_AND_BITWISE_XOR_ACQUIRE, args);
    }

    @Override
    public final Object getAndBitwiseXorRelease(Object... args) {
        return invoke(AccessMode.GET_AND_BITWISE_XOR_RELEASE, args);
    }

    //

    @Override
    public final Class<?> varType() {
        return varType;
    }

    @Override
    public final List<Class<?>> coordinateTypes() {
        return List.of(coordinates);
    }

    @Override
    public final MethodType accessModeType(AccessMode mode) {
        return modeMTTable[accessType(mode).ordinal()];
    }

    @Override
    public final boolean isAccessModeSupported(AccessMode accessMode) {
        int testBit = 1 << accessMode.ordinal();
        return (accessModesBitMask & testBit) == testBit;
    }

    @Override
    public final MethodHandle toMethodHandle(AccessMode mode) {
        return modeMHTable[mode.ordinal()];
    }

    //

    public enum AccessType {
        GET,
        SET,

        GET_ATOMIC,
        SET_ATOMIC,

        COMPARE_AND_SET,
        COMPARE_AND_EXCHANGE,
        GET_AND_UPDATE,

        GET_AND_UPDATE_BITWISE,
        GET_AND_UPDATE_NUMERIC;

        public MethodType accessModeType(Class<?> value, Class<?>... coordinates) {
            Class<?>[] ps;
            int i;
            switch (this) {
                case GET, GET_ATOMIC -> {
                    ps = allocateParameters(0, coordinates);
                    fillParameters(ps, coordinates);
                    return MethodType.methodType(value, ps);
                }
                case SET, SET_ATOMIC -> {
                    ps = allocateParameters(1, coordinates);
                    i = fillParameters(ps, coordinates);
                    ps[i] = value;
                    return MethodType.methodType(void.class, ps);
                }
                case COMPARE_AND_SET -> {
                    ps = allocateParameters(2, coordinates);
                    i = fillParameters(ps, coordinates);
                    ps[i++] = value;
                    ps[i] = value;
                    return MethodType.methodType(boolean.class, ps);
                }
                case COMPARE_AND_EXCHANGE -> {
                    ps = allocateParameters(2, coordinates);
                    i = fillParameters(ps, coordinates);
                    ps[i++] = value;
                    ps[i] = value;
                    return MethodType.methodType(value, ps);
                }
                case GET_AND_UPDATE, GET_AND_UPDATE_BITWISE, GET_AND_UPDATE_NUMERIC -> {
                    ps = allocateParameters(1, coordinates);
                    i = fillParameters(ps, coordinates);
                    ps[i] = value;
                    return MethodType.methodType(value, ps);
                }
                default -> throw new InternalError("Unknown AccessType");
            }
        }

        private static Class<?>[] allocateParameters(int values, Class<?>... coordinates) {
            int size = coordinates.length + values;
            return new Class<?>[size];
        }

        private static int fillParameters(Class<?>[] ps, Class<?>... coordinates) {
            int i = 0;
            for (Class<?> arg : coordinates) ps[i++] = arg;
            return i;
        }
    }

    @AlwaysInline
    public static AccessType accessType(AccessMode accessMode) {
        return VarHandle.accessType(accessMode);
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

    // implementation details

    @FunctionalInterface
    public interface VarHandleTransformer {
        void transform(AccessMode mode, EmulatedStackFrame stack) throws Throwable;
    }

    public static VarHandle newVarHandle(int accessModesBitMask, VarHandleTransformer impl,
                                         Class<?> varType, Class<?>... coordinates) {
        Objects.requireNonNull(impl);
        //TODO: cleanup
        return new VarHandleImpl(accessModesBitMask, (mode, type) ->
                Transformers.makeTransformer(type, new AbstractTransformer() {
                    @Override
                    protected void transform(MethodHandle ignored, EmulatedStackFrame stack) throws Throwable {
                        impl.transform(mode, stack);
                    }
                }), varType, coordinates);
    }

    @FunctionalInterface
    public interface VarHandleFactory {
        MethodHandle create(AccessMode mode, MethodType type);
    }

    private static final AccessType[] ALL_TYPES = AccessType.values();
    private static final AccessMode[] ALL_MODES = AccessMode.values();

    /**
     * BitMask of supported access mode indexed by AccessMode.ordinal().
     */
    private final int accessModesBitMask;
    /**
     * The target type for accesses.
     */
    private final Class<?> varType;
    /**
     * Coordinates of this VarHandle.
     */
    private final Class<?>[] coordinates;

    // cache tables
    private final MethodType[] modeMTTable;
    private final MethodHandle[] invokerMHTable;
    private final MethodHandle[] modeMHTable;

    VarHandleImpl(int accessModesBitMask, VarHandleFactory handleFactory,
                  Class<?> varType, Class<?>... coordinates) {
        this.accessModesBitMask = checkAccessModes(accessModesBitMask);
        this.varType = checkVarType(varType);
        this.coordinates = checkCoordinates(coordinates);

        this.modeMTTable = accessModeTypes();
        this.invokerMHTable = getInvokerHandles();
        this.modeMHTable = getMethodHandles(handleFactory);
    }

    public int getAccessModesBitMask() {
        return accessModesBitMask;
    }

    @AlwaysInline
    private static int checkAccessModes(int accessModesBitMask) {
        if ((accessModesBitMask & ~ALL_MODES_BIT_MASK) != 0) {
            throw new IllegalArgumentException(
                    "illegal accessModesBitMask: " + accessModesBitMask);
        }
        return accessModesBitMask;
    }

    @AlwaysInline
    private static Class<?> requireNonVoid(Class<?> clazz) {
        if (clazz == void.class) {
            throw new IllegalArgumentException("parameter type cannot be void");
        }
        return clazz;
    }

    @AlwaysInline
    private static Class<?> checkVarType(Class<?> varType) {
        return Objects.requireNonNull(requireNonVoid(varType));
    }

    @AlwaysInline
    private static Class<?>[] checkCoordinates(Class<?>[] coordinates) {
        Stream.of(Objects.requireNonNull(coordinates))
                .map(Objects::requireNonNull)
                .forEach(VarHandleImpl::requireNonVoid);
        return coordinates;
    }

    private static class UnsupportedAccessMode extends AbstractTransformer {
        private final AccessMode mode;

        public UnsupportedAccessMode(AccessMode mode) {
            this.mode = mode;
        }

        @Override
        protected void transform(MethodHandle ignored1, EmulatedStackFrame ignored2) {
            throw new UnsupportedOperationException("Unsupported access mode: " + mode);
        }
    }

    @AlwaysInline
    private MethodHandle[] getMethodHandles(VarHandleFactory handleFactory) {
        MethodHandle[] table = new MethodHandle[ALL_MODES.length];
        for (int i = 0; i < ALL_MODES.length; i++) {
            var mode = ALL_MODES[i];
            var type = accessModeType(mode);
            MethodHandle mh;
            if (isAccessModeSupported(mode)) {
                mh = handleFactory.create(mode, type);
            } else {
                mh = Transformers.makeTransformer(
                        type, new UnsupportedAccessMode(mode));
            }
            table[i] = mh;
        }
        return table;
    }

    @AlwaysInline
    private Object invoke(AccessMode mode, Object[] args) {
        return nothrows_run(() -> invokerMHTable[accessType(mode).ordinal()]
                .invokeExact(toMethodHandle(mode), args));
    }

    @AlwaysInline
    private MethodType accessModeTypeUncached(AccessType type) {
        return type.accessModeType(varType, coordinates);
    }

    @AlwaysInline
    private MethodType[] accessModeTypes() {
        MethodType[] table = new MethodType[ALL_TYPES.length];
        for (int i = 0; i < ALL_TYPES.length; i++) {
            table[i] = accessModeTypeUncached(ALL_TYPES[i]);
        }
        return table;
    }

    @AlwaysInline
    private MethodHandle getInvokerHandleUncached(AccessType accessType) {
        MethodType type = modeMTTable[accessType.ordinal()];
        MethodHandle invoker = MethodHandlesFixes.exactInvoker(type);
        invoker = MethodHandlesFixes.asType(invoker,
                type.generic().insertParameterTypes(0, MethodHandle.class));
        return invoker.asSpreader(Object[].class, type.parameterCount());
    }

    @AlwaysInline
    private MethodHandle[] getInvokerHandles() {
        MethodHandle[] table = new MethodHandle[ALL_TYPES.length];
        for (int i = 0; i < ALL_TYPES.length; i++) {
            table[i] = getInvokerHandleUncached(ALL_TYPES[i]);
        }
        return table;
    }
}
