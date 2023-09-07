package com.v7878.unsafe.invoke;

import static com.v7878.dex.bytecode.CodeBuilder.InvokeKind.DIRECT;
import static com.v7878.dex.bytecode.CodeBuilder.InvokeKind.STATIC;
import static com.v7878.dex.bytecode.CodeBuilder.InvokeKind.VIRTUAL;
import static com.v7878.dex.bytecode.CodeBuilder.Op.GET_OBJECT;
import static com.v7878.dex.bytecode.CodeBuilder.Op.PUT_OBJECT;
import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.AndroidUnsafe.allocateInstance;
import static com.v7878.unsafe.ArtMethodUtils.makeExecutablePublicNonFinal;
import static com.v7878.unsafe.ClassUtils.setClassStatus;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.DexFileUtils.setTrusted;
import static com.v7878.unsafe.Reflection.MethodHandleMirror;
import static com.v7878.unsafe.Reflection.arrayCast;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Reflection.unreflectDirect;
import static com.v7878.unsafe.Stack.getStackClass1;
import static com.v7878.unsafe.Utils.nothrows_run;

import android.util.ArrayMap;

import androidx.annotation.Keep;

import com.v7878.dex.ClassDef;
import com.v7878.dex.Dex;
import com.v7878.dex.EncodedField;
import com.v7878.dex.EncodedMethod;
import com.v7878.dex.FieldId;
import com.v7878.dex.MethodId;
import com.v7878.dex.ProtoId;
import com.v7878.dex.TypeId;
import com.v7878.unsafe.ClassUtils.ClassStatus;
import com.v7878.unsafe.Utils;
import com.v7878.unsafe.Utils.SoftReferenceCache;
import com.v7878.unsafe.invoke.EmulatedStackFrame.StackFrameAccessor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dalvik.system.DexFile;

@SuppressWarnings("deprecation")
public class Transformers {

    private static final Class<?> invoke_transformer = nothrows_run(
            () -> Class.forName("java.lang.invoke.Transformers$Transformer"));
    private static final MethodHandle directAsType = nothrows_run(() -> unreflectDirect(
            getDeclaredMethod(MethodHandle.class, "asType", MethodType.class)));
    private static final MethodHandle directAsVarargsCollector = nothrows_run(() -> unreflectDirect(
            getDeclaredMethod(MethodHandle.class, "asVarargsCollector", Class.class)));
    private static final MethodHandle directBindTo = nothrows_run(() -> unreflectDirect(
            getDeclaredMethod(MethodHandle.class, "bindTo", Object.class)));

    private static final Constructor<MethodHandle> transformer_constructor;
    private static final InvokerI invoker;

    static {
        TypeId mh = TypeId.of(MethodHandle.class);
        TypeId mt = TypeId.of(MethodType.class);

        TypeId esf = TypeId.of("dalvik.system.EmulatedStackFrame");
        TypeId mesf = TypeId.of(EmulatedStackFrame.class);

        //public final class Transformer extends MethodHandle implements Cloneable {
        //    TransformerImpl impl;
        //    <...>
        //}
        String transformer_name = Transformers.class.getName() + "$Transformer";
        TypeId transformer_id = TypeId.of(transformer_name);

        ClassDef transformer_def = new ClassDef(transformer_id);
        transformer_def.setSuperClass(TypeId.of(invoke_transformer));
        transformer_def.getInterfaces().add(TypeId.of(Cloneable.class));
        transformer_def.setAccessFlags(Modifier.PUBLIC | Modifier.FINAL);

        FieldId impl_field = new FieldId(transformer_id,
                TypeId.of(TransformerImpl.class), "impl");
        transformer_def.getClassData().getInstanceFields().add(
                new EncodedField(impl_field, Modifier.PUBLIC, null)
        );

        //public Transformer(MethodType type, TransformerImpl impl) {
        //    super(type);
        //    this.impl = impl;
        //}
        transformer_def.getClassData().getDirectMethods().add(new EncodedMethod(
                MethodId.constructor(transformer_id, mt, TypeId.of(TransformerImpl.class)),
                Modifier.PUBLIC | /*TODO: CONSTRUCTOR*/ 0x10000).withCode(0, b -> b
                .invoke(DIRECT, MethodId.constructor(TypeId.of(invoke_transformer), mt),
                        b.this_(), b.p(0))
                .iop(PUT_OBJECT, b.p(1), b.this_(), impl_field)
                .return_void()
        ));

        //public void transform(dalvik.system.EmulatedStackFrame stack) {
        //    impl.transform(com.v7878.unsafe.methodhandle.EmulatedStackFrame.wrap(stack));
        //}
        transformer_def.getClassData().getVirtualMethods().add(new EncodedMethod(
                new MethodId(transformer_id, new ProtoId(TypeId.V, esf), "transform"),
                Modifier.PUBLIC).withCode(2, b -> b
                .iop(GET_OBJECT, b.l(0), b.this_(), impl_field)
                .invoke(STATIC, new MethodId(mesf, new ProtoId(mesf,
                        TypeId.of(Object.class)), "wrap"), b.p(0))
                .move_result_object(b.l(1))
                .invoke(VIRTUAL, new MethodId(TypeId.of(TransformerImpl.class),
                                new ProtoId(TypeId.V, mh, mesf), "transform"),
                        b.l(0), b.this_(), b.l(1))
                .return_void()
        ));

        //public boolean isVarargsCollector() {
        //    return impl.isVarargsCollector(this);
        //}
        transformer_def.getClassData().getVirtualMethods().add(new EncodedMethod(
                new MethodId(transformer_id, new ProtoId(TypeId.Z), "isVarargsCollector"),
                Modifier.PUBLIC).withCode(1, b -> b
                .iop(GET_OBJECT, b.l(0), b.this_(), impl_field)
                .invoke(VIRTUAL, new MethodId(TypeId.of(TransformerImpl.class),
                                new ProtoId(TypeId.Z, mh), "isVarargsCollector"),
                        b.l(0), b.this_())
                .move_result(b.l(0))
                .return_(b.l(0))
        ));

        //public MethodHandle asVarargsCollector(Class<?> arrayType) {
        //    return impl.asVarargsCollector(this, arrayType);
        //}
        transformer_def.getClassData().getVirtualMethods().add(new EncodedMethod(
                new MethodId(transformer_id, new ProtoId(mh, TypeId.of(Class.class)),
                        "asVarargsCollector"), Modifier.PUBLIC).withCode(1, b -> b
                .iop(GET_OBJECT, b.l(0), b.this_(), impl_field)
                .invoke(VIRTUAL, new MethodId(TypeId.of(TransformerImpl.class), new ProtoId(mh,
                                mh, TypeId.of(Class.class)), "asVarargsCollector"),
                        b.l(0), b.this_(), b.p(0))
                .move_result_object(b.l(0))
                .return_object(b.l(0))
        ));

        //public MethodHandle asFixedArity() {
        //    return impl.asFixedArity(this);
        //}
        transformer_def.getClassData().getVirtualMethods().add(new EncodedMethod(
                new MethodId(transformer_id, new ProtoId(mh), "asFixedArity"),
                Modifier.PUBLIC).withCode(1, b -> b
                .iop(GET_OBJECT, b.l(0), b.this_(), impl_field)
                .invoke(VIRTUAL, new MethodId(TypeId.of(TransformerImpl.class),
                        new ProtoId(mh, mh), "asFixedArity"), b.l(0), b.this_())
                .move_result_object(b.l(0))
                .return_object(b.l(0))
        ));

        //public MethodHandle asType(MethodType type) {
        //    return impl.asType(this, type);
        //}
        transformer_def.getClassData().getVirtualMethods().add(new EncodedMethod(
                new MethodId(transformer_id, new ProtoId(mh, mt), "asType"),
                Modifier.PUBLIC).withCode(1, b -> b
                .iop(GET_OBJECT, b.l(0), b.this_(), impl_field)
                .invoke(VIRTUAL, new MethodId(TypeId.of(TransformerImpl.class),
                                new ProtoId(mh, mh, mt), "asType"),
                        b.l(0), b.this_(), b.p(0))
                .move_result_object(b.l(0))
                .return_object(b.l(0))
        ));

        //public MethodHandle bindTo(Object value) {
        //    return impl.bindTo(this, value);
        //}
        transformer_def.getClassData().getVirtualMethods().add(new EncodedMethod(
                new MethodId(transformer_id, new ProtoId(mh, TypeId.of(Object.class)), "bindTo"),
                Modifier.PUBLIC).withCode(1, b -> b
                .iop(GET_OBJECT, b.l(0), b.this_(), impl_field)
                .invoke(VIRTUAL, new MethodId(TypeId.of(TransformerImpl.class),
                                new ProtoId(mh, mh, TypeId.of(Object.class)), "bindTo"),
                        b.l(0), b.this_(), b.p(0))
                .move_result_object(b.l(0))
                .return_object(b.l(0))
        ));

        //public String toString() {
        //    return impl.toString(this);
        //}
        transformer_def.getClassData().getVirtualMethods().add(new EncodedMethod(
                new MethodId(transformer_id, new ProtoId(TypeId.of(String.class)), "toString"),
                Modifier.PUBLIC).withCode(1, b -> b
                .iop(GET_OBJECT, b.l(0), b.this_(), impl_field)
                .invoke(VIRTUAL, new MethodId(TypeId.of(TransformerImpl.class),
                                new ProtoId(TypeId.of(String.class), mh), "toString"),
                        b.l(0), b.this_())
                .move_result_object(b.l(0))
                .return_object(b.l(0))
        ));

        //public final class Invoker extends InvokerI {
        //    <...>
        //}
        String invoker_name = Transformers.class.getName() + "$Invoker";
        TypeId invoker_id = TypeId.of(invoker_name);

        ClassDef invoker_def = new ClassDef(invoker_id);
        invoker_def.setSuperClass(TypeId.of(InvokerI.class));
        invoker_def.setAccessFlags(Modifier.PUBLIC | Modifier.FINAL);


        //public void invokeExactWithFrame(MethodHandle handle, Object stack) {
        //    <...>
        //}
        invoker_def.getClassData().getVirtualMethods().add(new EncodedMethod(
                new MethodId(invoker_id, new ProtoId(TypeId.V, mh, TypeId.of(Object.class)),
                        "invokeExactWithFrame"), Modifier.PUBLIC).withCode(0, b -> {
            //b.check_cast(b.p(1), esf) // verified
            if (CORRECT_SDK_INT <= 32) {
                //handle.invoke((dalvik.system.EmulatedStackFrame) stack);
                b.invoke_polymorphic(new MethodId(mh, new ProtoId(TypeId.of(Object.class),
                                TypeId.of(Object[].class)), "invoke"),
                        new ProtoId(TypeId.V, esf), b.p(0), b.p(1));
            } else {
                Method tmp = getDeclaredMethod(MethodHandle.class,
                        "invokeExactWithFrame", EmulatedStackFrame.esf_class);
                makeExecutablePublicNonFinal(tmp);

                //handle.invokeExactWithFrame((dalvik.system.EmulatedStackFrame) stack);
                b.invoke(VIRTUAL, MethodId.of(tmp), b.p(0), b.p(1));
            }
            b.return_void();
        }));

        Method tmp = getDeclaredMethod(MethodHandle.class,
                "transform", EmulatedStackFrame.esf_class);
        makeExecutablePublicNonFinal(tmp);

        //public void transform(MethodHandle handle, Object stack) {
        //    handle.transform((dalvik.system.EmulatedStackFrame) stack);
        //}
        invoker_def.getClassData().getVirtualMethods().add(new EncodedMethod(
                new MethodId(invoker_id, new ProtoId(TypeId.V, TypeId.of(MethodHandle.class),
                        TypeId.of(Object.class)), "transform"),
                Modifier.PUBLIC).withCode(0, b -> b
                //.check_cast(b.p(1), esf) // verified
                .invoke(VIRTUAL, MethodId.of(tmp), b.p(0), b.p(1))
                .return_void()
        ));

        DexFile dex = openDexFile(new Dex(transformer_def, invoker_def).compile());
        setTrusted(dex);

        ClassLoader loader = Transformers.class.getClassLoader();

        Class<?> invoker_class = loadClass(dex, invoker_name, loader);
        setClassStatus(invoker_class, ClassStatus.Verified);
        invoker = (InvokerI) allocateInstance(invoker_class);

        Class<?> transformer = loadClass(dex, transformer_name, loader);
        //noinspection unchecked
        transformer_constructor = (Constructor<MethodHandle>) nothrows_run(() -> transformer
                .getDeclaredConstructor(MethodType.class, TransformerImpl.class));
    }

    private static MethodHandle makeTransformer(
            MethodType fixed, TransformerImpl impl, boolean variadic) {
        MethodHandle out = nothrows_run(() -> transformer_constructor.newInstance(fixed, impl));
        if (variadic && CORRECT_SDK_INT < 33) {
            //TODO: safer way
            MethodHandleMirror[] m = arrayCast(MethodHandleMirror.class, out);
            m[0].handleKind = /*INVOKE_CALLSITE_TRANSFORM*/ 6;
        }
        return out;
    }

    public static MethodHandle makeTransformer(MethodType type, TransformerF callback) {
        return makeTransformer(type, regularImpl(callback), false);
    }

    public static MethodHandle makeVariadicTransformer(TransformerF callback) {
        return makeTransformer(MethodType.methodType(void.class), variadicImpl(callback), true);
    }

    @Keep
    private abstract static class InvokerI {

        abstract void transform(MethodHandle handle, Object stackFrame) throws Throwable;

        abstract void invokeExactWithFrame(MethodHandle handle,
                                           Object stackFrame) throws Throwable;
    }

    @FunctionalInterface
    public interface TransformerI extends TransformerF {

        void transform(EmulatedStackFrame stackFrame) throws Throwable;


        default void transform(MethodHandle ignored, EmulatedStackFrame stackFrame) throws Throwable {
            transform(stackFrame);
        }
    }

    @Keep
    @FunctionalInterface
    public interface TransformerF {

        void transform(MethodHandle thiz, EmulatedStackFrame stackFrame) throws Throwable;
    }

    private static TransformerImpl regularImpl(TransformerF callback) {
        return new TransformerImpl() {
            @Override
            void transform(MethodHandle thiz, EmulatedStackFrame stackFrame) throws Throwable {
                callback.transform(thiz, stackFrame);
            }

            @Override
            boolean isVarargsCollector(MethodHandle thiz) {
                return false;
            }

            @Override
            MethodHandle asFixedArity(MethodHandle thiz) {
                return thiz;
            }

            @Override
            MethodHandle asType(MethodHandle thiz, MethodType newType) {
                return nothrows_run(() -> (MethodHandle) directAsType.invoke(thiz, newType));
            }

            @Override
            MethodHandle bindTo(MethodHandle thiz, Object value) {
                return nothrows_run(() -> (MethodHandle) directBindTo.invoke(thiz, value));
            }

            @Override
            MethodHandle asVarargsCollector(MethodHandle thiz, Class<?> arrayType) {
                return (MethodHandle) nothrows_run(
                        () -> directAsVarargsCollector.invoke(thiz, arrayType));
            }

            @Override
            String toString(MethodHandle thiz) {
                return "Transformer" + thiz.type();
            }
        };
    }

    private static TransformerImpl variadicImpl(TransformerF callback) {
        return new TransformerImpl() {

            @Override
            void transform(MethodHandle thiz, EmulatedStackFrame stackFrame) throws Throwable {
                callback.transform(thiz, stackFrame);
            }

            @Override
            boolean isVarargsCollector(MethodHandle ignored) {
                return true;
            }

            @Override
            MethodHandle asFixedArity(MethodHandle thiz) {
                return asType(thiz, thiz.type());
            }

            @Override
            MethodHandle asType(MethodHandle thiz, MethodType newType) {
                //TODO: maybe caching?
                return makeTransformer(newType, callback);
            }

            @Override
            MethodHandle bindTo(MethodHandle thiz, Object value) {
                //TODO
                throw new UnsupportedOperationException("Not implemented yet");
            }

            @Override
            MethodHandle asVarargsCollector(MethodHandle thiz, Class<?> arrayType) {
                return thiz;
            }

            @Override
            String toString(MethodHandle thiz) {
                return "Transformer(...)?";
            }
        };
    }

    @Keep
    @SuppressWarnings("unused")
    private abstract static class TransformerImpl {
        abstract void transform(MethodHandle thiz, EmulatedStackFrame stackFrame) throws Throwable;

        abstract boolean isVarargsCollector(MethodHandle thiz);

        abstract MethodHandle asVarargsCollector(MethodHandle thiz, Class<?> arrayType);

        abstract MethodHandle asFixedArity(MethodHandle thiz);

        abstract MethodHandle asType(MethodHandle thiz, MethodType newType);

        abstract MethodHandle bindTo(MethodHandle thiz, Object value);

        abstract String toString(MethodHandle thiz);
    }

    private static WrongMethodTypeException newWrongMethodTypeException(MethodType from, MethodType to) {
        return new WrongMethodTypeException("Cannot convert " + from + " to " + to);
    }

    public static void invokeExactWithFrameNoChecks(
            MethodHandle target, EmulatedStackFrame stackFrame) throws Throwable {
        if (invoke_transformer.isInstance(target)) {
            // FIXME: android 8-12L convert nominalType to type (PLATFORM-BUG!)
            invoker.transform(target, stackFrame.esf);
        } else {
            invoker.invokeExactWithFrame(target, stackFrame.esf);
        }
    }

    public static void invokeExactWithFrame(
            MethodHandle target, EmulatedStackFrame stackFrame) throws Throwable {
        if (!target.type().equals(stackFrame.type())) {
            throw newWrongMethodTypeException(stackFrame.type(), target.type());
        }
        invokeExactWithFrameNoChecks(target, stackFrame);
    }

    public static void invokeWithFrame(
            MethodHandle target, EmulatedStackFrame stackFrame) throws Throwable {
        MethodHandle adaptedTarget = target.asType(stackFrame.type());
        invokeExactWithFrameNoChecks(adaptedTarget, stackFrame);
    }

    private static class CollectArguments implements TransformerI {
        private final MethodHandle target;
        private final MethodHandle collector;
        private final int pos;
        private final int collector_count;

        CollectArguments(MethodHandle target, MethodHandle collector, int pos) {
            this.target = target;
            this.collector = collector;
            this.pos = pos;
            this.collector_count = collector.type().parameterCount();
        }

        @Override
        public void transform(EmulatedStackFrame stack) throws Throwable {
            StackFrameAccessor this_accessor = stack.createAccessor();

            // First invoke the collector.
            EmulatedStackFrame collectorFrame = EmulatedStackFrame.create(collector.type());
            StackFrameAccessor collector_accessor = collectorFrame.createAccessor();
            EmulatedStackFrame.copyArguments(this_accessor, pos,
                    collector_accessor, 0, collector_count);
            invokeExactWithFrameNoChecks(collector, collectorFrame);

            // Start constructing the target frame.
            EmulatedStackFrame targetFrame = EmulatedStackFrame.create(target.type());
            StackFrameAccessor target_accessor = targetFrame.createAccessor();
            EmulatedStackFrame.copyArguments(this_accessor, 0,
                    target_accessor, 0, pos);

            // If return type of collector is not void, we have a return value to copy.
            target_accessor.moveTo(pos);
            if (collector.type().returnType() != void.class) {
                EmulatedStackFrame.copyNext(collector_accessor.moveToReturn(),
                        target_accessor, collector.type().returnType());
            }

            // Finish constructing the target frame.
            int this_pos = pos + collector_count;
            EmulatedStackFrame.copyArguments(this_accessor, this_pos,
                    target_accessor, target_accessor.argumentIdx,
                    stack.type().parameterCount() - this_pos);

            // Invoke the target.
            invokeExactWithFrameNoChecks(target, targetFrame);
            targetFrame.copyReturnValueTo(stack);
        }
    }

    private static RuntimeException newIllegalArgumentException(String message, Object obj1, Object obj2) {
        return new IllegalArgumentException(message + ": " + obj1 + ", " + obj2);
    }

    // fix for PLATFORM-BUG!
    public static MethodHandle collectArguments(MethodHandle target, int pos, MethodHandle filter) {
        MethodType newType = collectArgumentsChecks(target, pos, filter);
        return makeTransformer(newType, new CollectArguments(target, filter, pos));
    }

    private static MethodType collectArgumentsChecks(MethodHandle target, int pos, MethodHandle filter) {
        MethodType targetType = target.type();
        MethodType filterType = filter.type();
        Class<?> rtype = filterType.returnType();
        List<Class<?>> filterArgs = filterType.parameterList();
        if (rtype == void.class) {
            return targetType.insertParameterTypes(pos, filterArgs);
        }
        if (rtype != targetType.parameterType(pos)) {
            throw newIllegalArgumentException("target and filter types do not match", targetType, filterType);
        }
        return targetType.dropParameterTypes(pos, pos + 1).insertParameterTypes(pos, filterArgs);
    }

    // fix for PLATFORM-BUG!
    public static MethodHandle identity(Class<?> type) {
        Objects.requireNonNull(type);
        return makeTransformer(MethodType.methodType(type, type), (TransformerI) stack -> {
            EmulatedStackFrame.copyNext(stack.createAccessor().moveTo(0),
                    stack.createAccessor().moveToReturn(), type);
        });
    }

    private static void addClass(Map<String, Class<?>> map, Class<?> clazz) {
        Class<?> component = clazz.getComponentType();
        while (component != null) {
            clazz = component;
            component = clazz.getComponentType();
        }

        if (clazz.getClassLoader() != null && clazz.getClassLoader() != Object.class.getClassLoader()) {
            map.put(clazz.getName(), clazz);
        }
    }

    private static ClassLoader getInvokerClassLoader(MethodType type) {
        ArrayMap<String, Class<?>> map = new ArrayMap<>(type.parameterCount() + 1);
        for (int i = 0; i < type.parameterCount(); i++) {
            addClass(map, type.parameterType(i));
        }
        addClass(map, type.returnType());

        if (map.size() == 0) {
            return Utils.newEmptyClassLoader();
        }

        // new every time, needed for GC
        return new ClassLoader(getStackClass1().getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                Class<?> out = map.get(name);
                if (out != null) {
                    return out;
                }
                return super.loadClass(name, resolve);
            }

            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                return loadClass(name, false);
            }
        };
    }

    private static String getInvokerName(ProtoId proto) {
        return Transformers.class.getName() + "$$$Invoker_" + proto.getShorty();
    }

    private static MethodHandle newInvoker(MethodType type, boolean exact) {
        ProtoId proto = ProtoId.of(type);
        MethodType itype = type.insertParameterTypes(0, MethodHandle.class);
        ProtoId iproto = ProtoId.of(itype);
        String invoker_name = getInvokerName(proto);
        TypeId invoker_id = TypeId.of(invoker_name);
        ClassDef invoker_def = new ClassDef(invoker_id);
        invoker_def.setSuperClass(TypeId.of(Object.class));

        MethodId invoke = new MethodId(TypeId.of(MethodHandle.class),
                new ProtoId(TypeId.of(Object.class), TypeId.of(Object[].class)),
                exact ? "invokeExact" : "invoke");

        MethodId iid = new MethodId(invoker_id, ProtoId.of(itype), "invoke");
        invoker_def.getClassData().getDirectMethods().add(new EncodedMethod(
                iid, Modifier.STATIC).withCode(2 /* locals for wide result */, b -> {
                    b.invoke_polymorphic_range(invoke, proto,
                            iproto.getInputRegistersCount(), b.p(0));
                    switch (iproto.getReturnType().getShorty()) {
                        case 'V':
                            b.return_void();
                            break;
                        case 'L':
                            b.move_result_object(b.l(0))
                                    .return_object(b.l(0));
                            break;
                        case 'J':
                        case 'D':
                            b.move_result_wide(b.l(0))
                                    .return_wide(b.l(0));
                            break;
                        default:
                            b.move_result(b.l(0))
                                    .return_(b.l(0));
                            break;
                    }
                }
        ));

        DexFile dex = openDexFile(new Dex(invoker_def).compile());
        Class<?> invoker = loadClass(dex, invoker_name, getInvokerClassLoader(type));

        return unreflect(getDeclaredMethod(invoker, "invoke", itype.parameterArray()));
    }

    private static final SoftReferenceCache<MethodType, MethodHandle>
            invokers_cache = new SoftReferenceCache<>();

    public static MethodHandle invoker(MethodType type) {
        Objects.requireNonNull(type);
        return invokers_cache.get(type, t -> newInvoker(t, false));
    }

    private static final SoftReferenceCache<MethodType, MethodHandle>
            exact_invokers_cache = new SoftReferenceCache<>();

    public static MethodHandle exactInvoker(MethodType type) {
        Objects.requireNonNull(type);
        return exact_invokers_cache.get(type, t -> newInvoker(t, true));
    }

    static class JustInvoke implements TransformerI {
        private final MethodHandle target;

        JustInvoke(MethodHandle target) {
            this.target = target;
        }

        @Override
        public void transform(EmulatedStackFrame stack) throws Throwable {
            // Invoke the target with checks
            invokeExactWithFrame(target, stack);
        }
    }

    public static MethodHandle protectHandle(MethodHandle target) {
        return makeTransformer(target.type(), new JustInvoke(target));
    }
}
