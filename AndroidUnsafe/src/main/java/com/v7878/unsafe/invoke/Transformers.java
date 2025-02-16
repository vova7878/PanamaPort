package com.v7878.unsafe.invoke;

import static com.v7878.dex.DexConstants.ACC_CONSTRUCTOR;
import static com.v7878.dex.DexConstants.ACC_FINAL;
import static com.v7878.dex.DexConstants.ACC_PRIVATE;
import static com.v7878.dex.DexConstants.ACC_PUBLIC;
import static com.v7878.dex.builder.CodeBuilder.InvokeKind.DIRECT;
import static com.v7878.dex.builder.CodeBuilder.InvokeKind.STATIC;
import static com.v7878.dex.builder.CodeBuilder.InvokeKind.SUPER;
import static com.v7878.dex.builder.CodeBuilder.InvokeKind.VIRTUAL;
import static com.v7878.dex.builder.CodeBuilder.Op.GET_OBJECT;
import static com.v7878.dex.builder.CodeBuilder.Op.PUT_OBJECT;
import static com.v7878.dex.builder.CodeBuilder.Test.EQ;
import static com.v7878.unsafe.AndroidUnsafe.allocateInstance;
import static com.v7878.unsafe.ArtFieldUtils.makeFieldPublic;
import static com.v7878.unsafe.ArtMethodUtils.makeExecutablePublic;
import static com.v7878.unsafe.ArtMethodUtils.makeMethodInheritable;
import static com.v7878.unsafe.ArtVersion.ART_SDK_INT;
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

import com.v7878.dex.DexIO;
import com.v7878.dex.builder.ClassBuilder;
import com.v7878.dex.builder.CodeBuilder;
import com.v7878.dex.immutable.ClassDef;
import com.v7878.dex.immutable.Dex;
import com.v7878.dex.immutable.FieldId;
import com.v7878.dex.immutable.MethodId;
import com.v7878.dex.immutable.ProtoId;
import com.v7878.dex.immutable.TypeId;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.unsafe.ApiSensitive;
import com.v7878.unsafe.ClassUtils.ClassStatus;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;

import dalvik.system.DexFile;

@ApiSensitive
public class Transformers {

    private static final MethodHandle directAsVarargsCollector = nothrows_run(() -> unreflectDirect(
            getDeclaredMethod(MethodHandle.class, "asVarargsCollector", Class.class)));
    private static final MethodHandle directBindTo = nothrows_run(() -> unreflectDirect(
            getDeclaredMethod(MethodHandle.class, "bindTo", Object.class)));

    private static final MethodHandle new_transformer;
    private static final InvokerI invoker;

    private static final boolean SKIP_CHECK_CAST = !DEBUG_BUILD;

    static {
        Class<?> invoke_transformer = nothrows_run(() -> Class.forName(
                "java.lang.invoke.Transformers$Transformer"));
        TypeId invoke_transformer_id = TypeId.of(invoke_transformer);

        TypeId mh = TypeId.of(MethodHandle.class);
        TypeId mt = TypeId.of(MethodType.class);

        TypeId esf = TypeId.ofName("dalvik.system.EmulatedStackFrame");
        TypeId mesf = TypeId.of(EmulatedStackFrame.class);

        String transformer_name = Transformers.class.getName() + "$Transformer";
        TypeId transformer_id = TypeId.ofName(transformer_name);

        TypeId transformer_impl_id = TypeId.of(TransformerImpl.class);

        FieldId impl_field = FieldId.of(transformer_id,
                "impl", transformer_impl_id);

        FieldId asTypeCache;
        if (ART_SDK_INT < 33) {
            asTypeCache = FieldId.of(transformer_id, "asTypeCache", mh);
        } else {
            Field cache = getDeclaredField(MethodHandle.class, "asTypeCache");
            makeFieldPublic(cache);
            asTypeCache = FieldId.of(cache);
        }

        // public final class Transformer extends MethodHandle implements Cloneable {
        //     <...>
        // }
        ClassDef transformer_def = ClassBuilder.build(transformer_id, cb -> cb
                .withSuperClass(invoke_transformer_id)
                .withInterfaces(TypeId.of(Cloneable.class))
                .withFlags(ACC_PUBLIC | ACC_FINAL)
                // private final TransformerImpl impl;
                .withField(fb -> fb
                        .of(impl_field)
                        .withFlags(ACC_PRIVATE | ACC_FINAL)
                )
                // public Transformer(MethodType type, int invokeKind, TransformerImpl impl) {
                //     super(type, invokeKind);
                //     this.impl = impl;
                // }
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC | ACC_CONSTRUCTOR)
                        .withConstructorSignature()
                        .withParameterTypes(mt, TypeId.I, transformer_impl_id)
                        .withCode(0, ib -> ib
                                .invoke(DIRECT, MethodId.constructor(invoke_transformer_id, mt, TypeId.I),
                                        ib.this_(), ib.p(0), ib.p(1))
                                .iop(PUT_OBJECT, ib.p(2), ib.this_(), impl_field)
                                .return_void()
                        )
                )
                // public void transform(dalvik.system.EmulatedStackFrame frame) {
                //     impl.transform(com.v7878.unsafe.invoke.EmulatedStackFrame.wrap(frame));
                // }
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC)
                        .withName("transform")
                        .withReturnType(TypeId.V)
                        .withParameterTypes(esf)
                        .withCode(1, ib -> ib
                                .iop(GET_OBJECT, ib.l(0), ib.this_(), impl_field)
                                .invoke(STATIC, MethodId.of(mesf, "wrap",
                                        ProtoId.of(mesf, TypeId.OBJECT)), ib.p(0))
                                .move_result_object(ib.p(0))
                                .invoke(VIRTUAL, MethodId.of(transformer_impl_id,
                                                "transform", ProtoId.of(TypeId.V, mh, mesf)),
                                        ib.l(0), ib.this_(), ib.p(0))
                                .return_void()
                        )
                )
                // public boolean isVarargsCollector() {
                //     return impl.isVarargsCollector(this);
                // }
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC)
                        .withName("isVarargsCollector")
                        .withReturnType(TypeId.Z)
                        .withParameters()
                        .withCode(1, ib -> ib
                                .iop(GET_OBJECT, ib.l(0), ib.this_(), impl_field)
                                .invoke(VIRTUAL, MethodId.of(transformer_impl_id,
                                                "isVarargsCollector", ProtoId.of(TypeId.Z, mh)),
                                        ib.l(0), ib.this_())
                                .move_result(ib.l(0))
                                .return_(ib.l(0))
                        )
                )
                // public MethodHandle asVarargsCollector(Class<?> arrayType) {
                //     return impl.asVarargsCollector(this, arrayType);
                // }
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC)
                        .withName("asVarargsCollector")
                        .withReturnType(mh)
                        .withParameterTypes(TypeId.of(Class.class))
                        .withCode(1, ib -> ib
                                .iop(GET_OBJECT, ib.l(0), ib.this_(), impl_field)
                                .invoke(VIRTUAL, MethodId.of(transformer_impl_id, "asVarargsCollector",
                                                ProtoId.of(mh, mh, TypeId.of(Class.class))),
                                        ib.l(0), ib.this_(), ib.p(0))
                                .move_result_object(ib.l(0))
                                .return_object(ib.l(0))
                        )
                )
                // public MethodHandle asFixedArity() {
                //     return impl.asFixedArity(this);
                // }
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC)
                        .withName("asFixedArity")
                        .withReturnType(mh)
                        .withParameters()
                        .withCode(1, ib -> ib
                                .iop(GET_OBJECT, ib.l(0), ib.this_(), impl_field)
                                .invoke(VIRTUAL, MethodId.of(transformer_impl_id, "asFixedArity",
                                        ProtoId.of(mh, mh)), ib.l(0), ib.this_())
                                .move_result_object(ib.l(0))
                                .return_object(ib.l(0))
                        )
                )
                // public MethodHandle bindTo(Object value) {
                //     return impl.bindTo(this, value);
                // }
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC)
                        .withName("bindTo")
                        .withReturnType(mh)
                        .withParameterTypes(TypeId.OBJECT)
                        .withCode(1, ib -> ib
                                .iop(GET_OBJECT, ib.l(0), ib.this_(), impl_field)
                                .invoke(VIRTUAL, MethodId.of(transformer_impl_id, "bindTo",
                                                ProtoId.of(mh, mh, TypeId.OBJECT)),
                                        ib.l(0), ib.this_(), ib.p(0))
                                .move_result_object(ib.l(0))
                                .return_object(ib.l(0))
                        )
                )
                // public String toString() {
                //     return impl.toString(this);
                // }
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC)
                        .withName("toString")
                        .withReturnType(TypeId.of(String.class))
                        .withParameters()
                        .withCode(1, ib -> ib
                                .iop(GET_OBJECT, ib.l(0), ib.this_(), impl_field)
                                .invoke(VIRTUAL, MethodId.of(transformer_impl_id, "toString",
                                        ProtoId.of(TypeId.of(String.class), mh)), ib.l(0), ib.this_())
                                .move_result_object(ib.l(0))
                                .return_object(ib.l(0))
                        )
                )
                .if_(ART_SDK_INT < 33, cb2 -> cb2
                        .withField(fb -> fb
                                .of(asTypeCache)
                                .withFlags(ACC_PRIVATE)
                        )
                )
                .commit(cb2 -> {
                    MethodId asTypeUncached = MethodId.of(transformer_id,
                            "asTypeUncached", ProtoId.of(mh, mt));
                    Consumer<CodeBuilder> fallbackAsType;
                    if (ART_SDK_INT < 33) {
                        if (ART_SDK_INT < 30) {
                            // return asTypeCache = super.asType(type);
                            fallbackAsType = ib -> ib
                                    .invoke(SUPER, MethodId.of(mh, "asType",
                                            ProtoId.of(mh, mt)), ib.this_(), ib.p(0))
                                    .move_result_object(ib.l(0))
                                    .iop(PUT_OBJECT, ib.l(0), ib.this_(), asTypeCache)
                                    .return_object(ib.l(0));
                        } else {
                            Method tmp = getDeclaredMethod(MethodHandle.class,
                                    "asTypeUncached", MethodType.class);
                            makeMethodInheritable(tmp);

                            // return asTypeCache = super.asTypeUncached(type);
                            fallbackAsType = ib -> ib
                                    .invoke(SUPER, MethodId.of(tmp), ib.this_(), ib.p(0))
                                    .move_result_object(ib.l(0))
                                    .iop(PUT_OBJECT, ib.l(0), ib.this_(), asTypeCache)
                                    .return_object(ib.l(0));
                        }

                        MethodId equals = MethodId.of(mt, "equals",
                                ProtoId.of(TypeId.Z, TypeId.OBJECT));
                        MethodId type = MethodId.of(mh, "type", ProtoId.of(mt));

                        // public MethodHandle asType(MethodType type) {
                        //     if (type.equals(this.type())) {
                        //         return this;
                        //     }
                        //     MethodHandle tmp = asTypeCache;
                        //     if (tmp != null && type.equals(tmp.type())) {
                        //         return tmp;
                        //     }
                        //     return asTypeUncached(type);
                        // }
                        cb2.withMethod(mb -> mb
                                .withFlags(ACC_PUBLIC)
                                .withName("asType")
                                .withReturnType(mh)
                                .withParameterTypes(mt)
                                .withCode(2, ib -> ib
                                        .invoke(VIRTUAL, type, ib.this_())
                                        .move_result_object(ib.l(0))
                                        .invoke(VIRTUAL, equals, ib.p(0), ib.l(0))
                                        .move_result(ib.l(0))
                                        .if_testz(EQ, ib.l(0), ":long_path")
                                        .return_object(ib.this_())

                                        .label(":long_path")
                                        .iop(GET_OBJECT, ib.l(0), ib.this_(), asTypeCache)
                                        .if_testz(EQ, ib.l(0), ":return_uncached")
                                        .invoke(VIRTUAL, type, ib.l(0))
                                        .move_result_object(ib.l(1))
                                        .invoke(VIRTUAL, equals, ib.p(0), ib.l(1))
                                        .move_result(ib.l(1))
                                        .if_testz(EQ, ib.l(1), ":return_uncached")
                                        .return_object(ib.l(0))

                                        .label(":return_uncached")
                                        .invoke(VIRTUAL, asTypeUncached, ib.this_(), ib.p(0))
                                        .move_result_object(ib.l(0))
                                        .return_object(ib.l(0))
                                )
                        );
                    } else {
                        Method uncached = getDeclaredMethod(MethodHandle.class,
                                "asTypeUncached", MethodType.class);
                        makeMethodInheritable(uncached);
                        // return super.asTypeUncached(type);
                        fallbackAsType = ib -> ib
                                .invoke(SUPER, MethodId.of(uncached), ib.this_(), ib.p(0))
                                .move_result_object(ib.l(0))
                                .return_object(ib.l(0));
                    }
                    // public MethodHandle asTypeUncached(MethodType type) {
                    //     MethodHandle tmp = impl.asType(this, type);
                    //     if (tmp == null) {
                    //         <fallback code>
                    //     }
                    //     return asTypeCache = tmp;
                    // }
                    cb2.withMethod(mb -> mb
                            .of(asTypeUncached)
                            .withFlags(ACC_PUBLIC)
                            .withCode(1, ib -> ib
                                    .iop(GET_OBJECT, ib.l(0), ib.this_(), impl_field)
                                    .invoke(VIRTUAL, MethodId.of(TypeId.of(TransformerImpl.class),
                                                    "asTypeUncached", ProtoId.of(mh, mh, mt)),
                                            ib.l(0), ib.this_(), ib.p(0))
                                    .move_result_object(ib.l(0))
                                    .if_testz(EQ, ib.l(0), ":null")
                                    .iop(PUT_OBJECT, ib.l(0), ib.this_(), asTypeCache)
                                    .return_object(ib.l(0))
                                    .label(":null")
                                    .commit(fallbackAsType)
                            )
                    );
                })
        );

        String invoker_name = Transformers.class.getName() + "$Invoker";
        TypeId invoker_id = TypeId.ofName(invoker_name);

        // public final class Invoker extends InvokerI {
        //     <...>
        // }
        ClassDef invoker_def = ClassBuilder.build(invoker_id, cb -> cb
                .withSuperClass(TypeId.of(InvokerI.class))
                .withFlags(ACC_PUBLIC | ACC_FINAL)
                // public boolean isTransformer(MethodHandle handle) {
                //     return handle instanceof Transformer;
                // }
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC)
                        .withName("isTransformer")
                        .withReturnType(TypeId.Z)
                        .withParameterTypes(mh)
                        .withCode(0, ib -> ib
                                .instance_of(ib.p(0), ib.p(0), invoke_transformer_id)
                                .return_(ib.p(0))
                        )
                )
                .commit(cb2 -> {
                    Method transform = getDeclaredMethod(MethodHandle.class,
                            "transform", EmulatedStackFrame.esf_class);
                    makeMethodInheritable(transform);
                    // public void transform(MethodHandle handle, Object stack) {
                    //     handle.transform((dalvik.system.EmulatedStackFrame) stack);
                    // }
                    cb2.withMethod(mb -> mb
                            .withFlags(ACC_PUBLIC)
                            .withName("transform")
                            .withReturnType(TypeId.V)
                            .withParameterTypes(mh, TypeId.OBJECT)
                            .withCode(0, ib -> ib
                                    .if_(!SKIP_CHECK_CAST, ib2 -> ib2
                                            .check_cast(ib.p(1), esf)
                                    )
                                    .invoke(VIRTUAL, MethodId.of(transform), ib.p(0), ib.p(1))
                                    .return_void()
                            )
                    );
                })
                // public void invokeExactWithFrame(MethodHandle handle, Object stack) {
                //     if (ART_SDK_INT <= 32) {
                //         handle.invoke((dalvik.system.EmulatedStackFrame) stack);
                //     } else {
                //         handle.invokeExactWithFrame((dalvik.system.EmulatedStackFrame) stack);
                //     }
                // }
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC)
                        .withName("invokeExactWithFrame")
                        .withReturnType(TypeId.V)
                        .withParameterTypes(mh, TypeId.OBJECT)
                        .withCode(0, b -> {
                            if (!SKIP_CHECK_CAST) {
                                b.check_cast(b.p(1), esf);
                            }
                            if (ART_SDK_INT <= 32) {
                                b.invoke_polymorphic(MethodId.of(mh, "invoke",
                                                ProtoId.of(TypeId.OBJECT, TypeId.of(Object[].class))),
                                        ProtoId.of(TypeId.V, esf), b.p(0), b.p(1));
                            } else {
                                Method invokeWF = getDeclaredMethod(MethodHandle.class,
                                        "invokeExactWithFrame", EmulatedStackFrame.esf_class);
                                makeExecutablePublic(invokeWF);
                                b.invoke(VIRTUAL, MethodId.of(invokeWF), b.p(0), b.p(1));
                            }
                            b.return_void();
                        })
                )
        );

        DexFile dex = openDexFile(DexIO.write(Dex.of(transformer_def, invoker_def)));
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
        final int INVOKE_TRANSFORM_26_36 = 5;
        int kind = variadic && ART_SDK_INT < 33 ?
                INVOKE_CALLSITE_TRANSFORM_26_32 : INVOKE_TRANSFORM_26_36;
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
        abstract boolean isTransformer(MethodHandle handle);

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

    public static boolean isTransformer(MethodHandle handle) {
        return invoker.isTransformer(Objects.requireNonNull(handle));
    }

    @ApiSensitive
    public static void invokeExactWithFrameNoChecks(
            MethodHandle target, EmulatedStackFrame stackFrame) throws Throwable {
        if (invoker.isTransformer(target)) {
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
