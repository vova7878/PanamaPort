package com.v7878.unsafe.invoke;

import static com.v7878.dex.DexConstants.ACC_CONSTRUCTOR;
import static com.v7878.dex.DexConstants.ACC_FINAL;
import static com.v7878.dex.DexConstants.ACC_PRIVATE;
import static com.v7878.dex.DexConstants.ACC_PUBLIC;
import static com.v7878.dex.bytecode.CodeBuilder.InvokeKind.DIRECT;
import static com.v7878.dex.bytecode.CodeBuilder.InvokeKind.STATIC;
import static com.v7878.dex.bytecode.CodeBuilder.InvokeKind.SUPER;
import static com.v7878.dex.bytecode.CodeBuilder.InvokeKind.VIRTUAL;
import static com.v7878.dex.bytecode.CodeBuilder.Op.GET_OBJECT;
import static com.v7878.dex.bytecode.CodeBuilder.Op.PUT_OBJECT;
import static com.v7878.dex.bytecode.CodeBuilder.Test.EQ;
import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.AndroidUnsafe.allocateInstance;
import static com.v7878.unsafe.ArtFieldUtils.makeFieldPublic;
import static com.v7878.unsafe.ArtMethodUtils.makeExecutablePublic;
import static com.v7878.unsafe.ArtMethodUtils.makeMethodInheritable;
import static com.v7878.unsafe.ClassUtils.setClassStatus;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.DexFileUtils.setTrusted;
import static com.v7878.unsafe.Reflection.getDeclaredConstructor;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Reflection.unreflectDirect;
import static com.v7878.unsafe.Utils.DEBUG_BUILD;
import static com.v7878.unsafe.Utils.newWrongMethodTypeException;
import static com.v7878.unsafe.Utils.nothrows_run;

import com.v7878.dex.ClassDef;
import com.v7878.dex.Dex;
import com.v7878.dex.EncodedField;
import com.v7878.dex.EncodedMethod;
import com.v7878.dex.FieldId;
import com.v7878.dex.MethodId;
import com.v7878.dex.ProtoId;
import com.v7878.dex.TypeId;
import com.v7878.dex.bytecode.CodeBuilder;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.unsafe.ApiSensitive;
import com.v7878.unsafe.ClassUtils.ClassStatus;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;

import dalvik.system.DexFile;

@ApiSensitive
public class Transformers {

    public static final Class<?> INVOKE_TRANSFORMER = nothrows_run(
            () -> Class.forName("java.lang.invoke.Transformers$Transformer"));

    private static final MethodHandle directAsVarargsCollector = nothrows_run(() -> unreflectDirect(
            getDeclaredMethod(MethodHandle.class, "asVarargsCollector", Class.class)));
    private static final MethodHandle directBindTo = nothrows_run(() -> unreflectDirect(
            getDeclaredMethod(MethodHandle.class, "bindTo", Object.class)));

    private static final MethodHandle new_transformer;
    private static final InvokerI invoker;

    private static final boolean SKIP_CHECK_CAST = !DEBUG_BUILD;

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
        transformer_def.setSuperClass(TypeId.of(INVOKE_TRANSFORMER));
        transformer_def.getInterfaces().add(TypeId.of(Cloneable.class));
        transformer_def.setAccessFlags(ACC_PUBLIC | ACC_FINAL);

        FieldId impl_field = new FieldId(transformer_id,
                TypeId.of(TransformerImpl.class), "impl");
        transformer_def.getClassData().getInstanceFields().add(
                new EncodedField(impl_field, ACC_PRIVATE, null)
        );

        //public Transformer(MethodType type, int invokeKind, TransformerImpl impl) {
        //    super(type, invokeKind);
        //    this.impl = impl;
        //}
        transformer_def.getClassData().getDirectMethods().add(new EncodedMethod(
                MethodId.constructor(transformer_id, mt, TypeId.I, TypeId.of(TransformerImpl.class)),
                ACC_PUBLIC | ACC_CONSTRUCTOR).withCode(0, b -> b
                .invoke(DIRECT, MethodId.constructor(TypeId.of(INVOKE_TRANSFORMER), mt, TypeId.I),
                        b.this_(), b.p(0), b.p(1))
                .iop(PUT_OBJECT, b.p(2), b.this_(), impl_field)
                .return_void()
        ));

        //public void transform(dalvik.system.EmulatedStackFrame stack) {
        //    impl.transform(com.v7878.unsafe.methodhandle.EmulatedStackFrame.wrap(stack));
        //}
        transformer_def.getClassData().getVirtualMethods().add(new EncodedMethod(
                new MethodId(transformer_id, new ProtoId(TypeId.V, esf), "transform"),
                ACC_PUBLIC).withCode(2, b -> b
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
                ACC_PUBLIC).withCode(1, b -> b
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
                        "asVarargsCollector"), ACC_PUBLIC).withCode(1, b -> b
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
                ACC_PUBLIC).withCode(1, b -> b
                .iop(GET_OBJECT, b.l(0), b.this_(), impl_field)
                .invoke(VIRTUAL, new MethodId(TypeId.of(TransformerImpl.class),
                        new ProtoId(mh, mh), "asFixedArity"), b.l(0), b.this_())
                .move_result_object(b.l(0))
                .return_object(b.l(0))
        ));

        //public MethodHandle bindTo(Object value) {
        //    return impl.bindTo(this, value);
        //}
        transformer_def.getClassData().getVirtualMethods().add(new EncodedMethod(
                new MethodId(transformer_id, new ProtoId(mh, TypeId.of(Object.class)), "bindTo"),
                ACC_PUBLIC).withCode(1, b -> b
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
                ACC_PUBLIC).withCode(1, b -> b
                .iop(GET_OBJECT, b.l(0), b.this_(), impl_field)
                .invoke(VIRTUAL, new MethodId(TypeId.of(TransformerImpl.class),
                                new ProtoId(TypeId.of(String.class), mh), "toString"),
                        b.l(0), b.this_())
                .move_result_object(b.l(0))
                .return_object(b.l(0))
        ));

        FieldId asTypeCache;
        if (CORRECT_SDK_INT < 33) {
            asTypeCache = new FieldId(transformer_id, mh, "asTypeCache");
            transformer_def.getClassData().getInstanceFields().add(
                    new EncodedField(asTypeCache, ACC_PRIVATE, null)
            );
        } else {
            Field tmp = getDeclaredField(MethodHandle.class, "asTypeCache");
            makeFieldPublic(tmp);
            asTypeCache = FieldId.of(tmp);
        }

        MethodId asTypeUncached = new MethodId(transformer_id, new ProtoId(mh, mt), "asTypeUncached");
        Consumer<CodeBuilder> fallbackAsType;
        if (CORRECT_SDK_INT < 33) {
            if (CORRECT_SDK_INT < 30) {
                //return asTypeCache = super.asType(type);
                fallbackAsType = b -> b
                        .invoke(SUPER, new MethodId(mh, new ProtoId(mh, mt),
                                "asType"), b.this_(), b.p(0))
                        .move_result_object(b.l(0))
                        .iop(PUT_OBJECT, b.l(0), b.this_(), asTypeCache)
                        .return_object(b.l(0));
            } else {
                Method tmp = getDeclaredMethod(MethodHandle.class,
                        "asTypeUncached", MethodType.class);
                makeMethodInheritable(tmp);

                //return asTypeCache = super.asTypeUncached(type);
                fallbackAsType = b -> b
                        .invoke(SUPER, MethodId.of(tmp), b.this_(), b.p(0))
                        .move_result_object(b.l(0))
                        .iop(PUT_OBJECT, b.l(0), b.this_(), asTypeCache)
                        .return_object(b.l(0));
            }


            MethodId equals = new MethodId(mt, new ProtoId(TypeId.Z,
                    TypeId.of(Object.class)), "equals");
            MethodId type = new MethodId(mh, new ProtoId(mt), "type");

            //public MethodHandle asType(MethodType type) {
            //    if (type.equals(this.type())) {
            //        return this;
            //    }
            //    MethodHandle tmp = asTypeCache;
            //    if (tmp != null && type.equals(tmp.type())) {
            //        return tmp;
            //    }
            //    return asTypeUncached(type);
            //}
            transformer_def.getClassData().getVirtualMethods().add(new EncodedMethod(
                    new MethodId(transformer_id, new ProtoId(mh, mt), "asType"),
                    ACC_PUBLIC).withCode(2, b -> b

                    .invoke(VIRTUAL, type, b.this_())
                    .move_result_object(b.l(0))
                    .invoke(VIRTUAL, equals, b.p(0), b.l(0))
                    .move_result(b.l(0))
                    .if_testz(EQ, b.l(0), ":long_path")
                    .return_object(b.this_())

                    .label(":long_path")
                    .iop(GET_OBJECT, b.l(0), b.this_(), asTypeCache)
                    .if_testz(EQ, b.l(0), ":return_uncached")
                    .invoke(VIRTUAL, type, b.l(0))
                    .move_result_object(b.l(1))
                    .invoke(VIRTUAL, equals, b.p(0), b.l(1))
                    .move_result(b.l(1))
                    .if_testz(EQ, b.l(1), ":return_uncached")
                    .return_object(b.l(0))

                    .label(":return_uncached")
                    .invoke(VIRTUAL, asTypeUncached, b.this_(), b.p(0))
                    .move_result_object(b.l(0))
                    .return_object(b.l(0))
            ));
        } else {
            Method tmp = getDeclaredMethod(MethodHandle.class,
                    "asTypeUncached", MethodType.class);
            makeMethodInheritable(tmp);

            //return super.asTypeUncached(type);
            fallbackAsType = b -> b
                    .invoke(SUPER, MethodId.of(tmp), b.this_(), b.p(0))
                    .move_result_object(b.l(0))
                    .return_object(b.l(0));
        }

        //public MethodHandle asTypeUncached(MethodType type) {
        //    MethodHandle tmp = impl.asType(this, type);
        //    if (tmp == null) {
        //        <fallback code>
        //    }
        //    return asTypeCache = tmp;
        //}
        transformer_def.getClassData().getVirtualMethods().add(new EncodedMethod(asTypeUncached,
                ACC_PUBLIC).withCode(1, b -> b
                .iop(GET_OBJECT, b.l(0), b.this_(), impl_field)
                .invoke(VIRTUAL, new MethodId(TypeId.of(TransformerImpl.class),
                                new ProtoId(mh, mh, mt), "asTypeUncached"),
                        b.l(0), b.this_(), b.p(0))
                .move_result_object(b.l(0))
                .if_testz(EQ, b.l(0), ":null")
                .iop(PUT_OBJECT, b.l(0), b.this_(), asTypeCache)
                .return_object(b.l(0))
                .label(":null")
                .commit(fallbackAsType)
        ));

        //public final class Invoker extends InvokerI {
        //    <...>
        //}
        String invoker_name = Transformers.class.getName() + "$Invoker";
        TypeId invoker_id = TypeId.of(invoker_name);

        ClassDef invoker_def = new ClassDef(invoker_id);
        invoker_def.setSuperClass(TypeId.of(InvokerI.class));
        invoker_def.setAccessFlags(ACC_PUBLIC | ACC_FINAL);


        //public void invokeExactWithFrame(MethodHandle handle, Object stack) {
        //    <...>
        //}
        invoker_def.getClassData().getVirtualMethods().add(new EncodedMethod(
                new MethodId(invoker_id, new ProtoId(TypeId.V, mh, TypeId.of(Object.class)),
                        "invokeExactWithFrame"), ACC_PUBLIC).withCode(0, b -> {
            if (!SKIP_CHECK_CAST) {
                b.check_cast(b.p(1), esf);
            }
            if (CORRECT_SDK_INT <= 32) {
                //handle.invoke((dalvik.system.EmulatedStackFrame) stack);
                b.invoke_polymorphic(new MethodId(mh, new ProtoId(TypeId.of(Object.class),
                                TypeId.of(Object[].class)), "invoke"),
                        new ProtoId(TypeId.V, esf), b.p(0), b.p(1));
            } else {
                Method tmp = getDeclaredMethod(MethodHandle.class,
                        "invokeExactWithFrame", EmulatedStackFrame.esf_class);
                makeExecutablePublic(tmp);

                //handle.invokeExactWithFrame((dalvik.system.EmulatedStackFrame) stack);
                b.invoke(VIRTUAL, MethodId.of(tmp), b.p(0), b.p(1));
            }
            b.return_void();
        }));

        Method tmp = getDeclaredMethod(MethodHandle.class,
                "transform", EmulatedStackFrame.esf_class);
        makeMethodInheritable(tmp);

        //public void transform(MethodHandle handle, Object stack) {
        //    handle.transform((dalvik.system.EmulatedStackFrame) stack);
        //}
        invoker_def.getClassData().getVirtualMethods().add(new EncodedMethod(
                new MethodId(invoker_id, new ProtoId(TypeId.V, TypeId.of(MethodHandle.class),
                        TypeId.of(Object.class)), "transform"),
                ACC_PUBLIC).withCode(0, b -> {
                    if (!SKIP_CHECK_CAST) {
                        b.check_cast(b.p(1), esf);
                    }
                    b.invoke(VIRTUAL, MethodId.of(tmp), b.p(0), b.p(1));
                    b.return_void();
                }
        ));

        DexFile dex = openDexFile(new Dex(transformer_def, invoker_def).compile());
        setTrusted(dex);

        ClassLoader loader = Transformers.class.getClassLoader();

        Class<?> invoker_class = loadClass(dex, invoker_name, loader);
        if (SKIP_CHECK_CAST) {
            setClassStatus(invoker_class, ClassStatus.Verified);
        }
        invoker = (InvokerI) allocateInstance(invoker_class);

        Class<?> transformer = loadClass(dex, transformer_name, loader);
        new_transformer = unreflect(getDeclaredConstructor(transformer,
                MethodType.class, int.class, TransformerImpl.class));
    }

    @ApiSensitive
    private static MethodHandle makeTransformer(
            MethodType fixed, TransformerImpl impl, boolean variadic) {
        final int INVOKE_CALLSITE_TRANSFORM_26_32 = 6;
        final int INVOKE_TRANSFORM_26_35 = 5;
        int kind = variadic && CORRECT_SDK_INT < 33 ?
                INVOKE_CALLSITE_TRANSFORM_26_32 : INVOKE_TRANSFORM_26_35;
        return nothrows_run(() -> (MethodHandle) new_transformer.invoke(fixed, kind, impl));
    }

    public static MethodHandle makeTransformer(MethodType type, AbstractTransformer callback) {
        return makeTransformer(type, callback, false);
    }

    public static MethodHandle makeVariadicTransformer(AbstractVariadicTransformer callback) {
        return makeTransformer(MethodType.methodType(void.class), callback, true);
    }

    @DoNotShrink
    @DoNotObfuscate
    private abstract static class InvokerI {
        abstract void transform(MethodHandle handle, Object stackFrame) throws Throwable;

        abstract void invokeExactWithFrame(MethodHandle handle,
                                           Object stackFrame) throws Throwable;
    }

    @DoNotShrink
    @DoNotObfuscate
    @SuppressWarnings("unused")
    private abstract static class TransformerImpl {
        protected abstract void transform(MethodHandle thiz, EmulatedStackFrame stackFrame) throws Throwable;

        protected abstract boolean isVarargsCollector(MethodHandle thiz);

        protected abstract MethodHandle asVarargsCollector(MethodHandle thiz, Class<?> arrayType);

        protected abstract MethodHandle asFixedArity(MethodHandle thiz);

        protected abstract MethodHandle asTypeUncached(MethodHandle thiz, MethodType newType);

        protected abstract MethodHandle bindTo(MethodHandle thiz, Object value);

        protected abstract String toString(MethodHandle thiz);
    }

    public static abstract class AbstractTransformer extends TransformerImpl {
        @Override
        protected abstract void transform(MethodHandle thiz, EmulatedStackFrame stackFrame) throws Throwable;

        @Override
        protected boolean isVarargsCollector(MethodHandle thiz) {
            return false;
        }

        @Override
        protected MethodHandle asFixedArity(MethodHandle thiz) {
            return thiz;
        }

        @Override
        protected MethodHandle asTypeUncached(MethodHandle thiz, MethodType newType) {
            return null; // fallback realization
        }

        @Override
        protected MethodHandle bindTo(MethodHandle thiz, Object value) {
            return nothrows_run(() -> (MethodHandle) directBindTo.invoke(thiz, value));
        }

        @Override
        protected MethodHandle asVarargsCollector(MethodHandle thiz, Class<?> arrayType) {
            return (MethodHandle) nothrows_run(
                    () -> directAsVarargsCollector.invoke(thiz, arrayType));
        }

        @Override
        protected String toString(MethodHandle thiz) {
            return "Transformer" + thiz.type();
        }
    }

    private static class VariadicAsRegular extends AbstractTransformer {
        private final AbstractVariadicTransformer target;

        VariadicAsRegular(AbstractVariadicTransformer target) {
            this.target = target;
        }

        @Override
        protected void transform(MethodHandle thiz, EmulatedStackFrame stackFrame) throws Throwable {
            target.transform(thiz, stackFrame);
        }
    }

    public static abstract class AbstractVariadicTransformer extends TransformerImpl {
        @Override
        protected abstract void transform(MethodHandle thiz, EmulatedStackFrame stackFrame) throws Throwable;

        @Override
        protected boolean isVarargsCollector(MethodHandle ignored) {
            return true;
        }

        @Override
        protected MethodHandle asFixedArity(MethodHandle thiz) {
            return asTypeUncached(thiz, thiz.type());
        }

        @Override
        protected MethodHandle asTypeUncached(MethodHandle thiz, MethodType newType) {
            return makeTransformer(newType, new VariadicAsRegular(this));
        }

        @Override
        protected MethodHandle bindTo(MethodHandle thiz, Object value) {
            //TODO
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        protected MethodHandle asVarargsCollector(MethodHandle thiz, Class<?> arrayType) {
            return thiz;
        }

        @Override
        protected String toString(MethodHandle thiz) {
            return "Transformer(...)?";
        }
    }

    @ApiSensitive
    public static void invokeExactWithFrameNoChecks(
            MethodHandle target, EmulatedStackFrame stackFrame) throws Throwable {
        if (INVOKE_TRANSFORMER.isInstance(target)) {
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
}
