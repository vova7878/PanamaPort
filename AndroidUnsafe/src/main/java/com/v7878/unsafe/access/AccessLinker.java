package com.v7878.unsafe.access;

import static com.v7878.dex.DexConstants.ACC_FINAL;
import static com.v7878.dex.DexConstants.ACC_INTERFACE;
import static com.v7878.dex.DexConstants.ACC_PUBLIC;
import static com.v7878.dex.builder.CodeBuilder.InvokeKind.DIRECT;
import static com.v7878.dex.builder.CodeBuilder.InvokeKind.INTERFACE;
import static com.v7878.dex.builder.CodeBuilder.InvokeKind.STATIC;
import static com.v7878.dex.builder.CodeBuilder.InvokeKind.VIRTUAL;
import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.ArtFieldUtils.makeFieldNonFinal;
import static com.v7878.unsafe.ArtFieldUtils.makeFieldPublic;
import static com.v7878.unsafe.ArtMethodUtils.makeExecutablePublic;
import static com.v7878.unsafe.ArtVersion.ART_SDK_INT;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.DexFileUtils.setTrusted;
import static com.v7878.unsafe.Reflection.getDeclaredMethods;
import static com.v7878.unsafe.Utils.searchExecutable;
import static com.v7878.unsafe.Utils.searchField;
import static com.v7878.unsafe.access.AccessLinker.ExecutableAccessKind.INVOKE_CONSTRUCTOR;
import static com.v7878.unsafe.access.AccessLinker.ExecutableAccessKind.NEW_INSTANCE;
import static com.v7878.unsafe.access.AccessLinker.FieldAccessKind.INSTANCE_SETTER;
import static com.v7878.unsafe.access.AccessLinker.FieldAccessKind.STATIC_SETTER;
import static java.lang.annotation.ElementType.METHOD;

import com.v7878.dex.DexIO;
import com.v7878.dex.builder.ClassBuilder;
import com.v7878.dex.immutable.ClassDef;
import com.v7878.dex.immutable.Dex;
import com.v7878.dex.immutable.FieldId;
import com.v7878.dex.immutable.MethodId;
import com.v7878.dex.immutable.ProtoId;
import com.v7878.dex.immutable.TypeId;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.ApiSensitive;
import com.v7878.unsafe.ClassUtils;
import com.v7878.unsafe.DexFileUtils;
import com.v7878.unsafe.Reflection;
import com.v7878.unsafe.Utils;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dalvik.system.DexFile;

public class AccessLinker {
    // TODO: ClassAccess (instanceof, checkcast, new_instance)

    public enum ExecutableAccessKind {
        VIRTUAL, INTERFACE, STATIC,
        // just call constructor for an existing object or create a new instance first
        INVOKE_CONSTRUCTOR, NEW_INSTANCE,
        // NOTE: invoke-direct access is impossible except for constructors
        // TODO: DIRECT_AS_VIRTUAL (As a workaround for invoke-direct)
        // TODO: invoke-polymorphic?
    }

    @DoNotShrink
    @DoNotShrinkType
    public @interface Conditions {
        @ApiSensitive
        int min_api() default 0;

        @ApiSensitive
        int max_api() default Integer.MAX_VALUE;

        @ApiSensitive
        int min_art() default 0;

        @ApiSensitive
        int max_art() default Integer.MAX_VALUE;
    }

    private static boolean checkConditions(Conditions cond) {
        return (cond.min_api() <= CORRECT_SDK_INT && cond.max_api() >= CORRECT_SDK_INT) &&
                (cond.min_art() <= ART_SDK_INT && cond.max_art() >= ART_SDK_INT);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    @Repeatable(ExecutableAccesses.class)
    @DoNotShrink
    @DoNotShrinkType
    public @interface ExecutableAccess {
        Conditions conditions() default @Conditions();

        ExecutableAccessKind kind();

        String klass();

        String name();

        String[] args();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    @DoNotShrink
    @DoNotShrinkType
    public @interface ExecutableAccesses {
        ExecutableAccess[] value();
    }

    private static ExecutableAccess getExecutableAccess(ExecutableAccess[] annotations) {
        for (var annotation : annotations) {
            if (checkConditions(annotation.conditions())) {
                return annotation;
            }
        }
        return null;
    }

    public enum FieldAccessKind {
        INSTANCE_GETTER, INSTANCE_SETTER,
        STATIC_GETTER, STATIC_SETTER
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    @Repeatable(FieldAccesses.class)
    @DoNotShrink
    @DoNotShrinkType
    public @interface FieldAccess {
        Conditions conditions() default @Conditions();

        FieldAccessKind kind();

        String klass();

        String name();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    @DoNotShrink
    @DoNotShrinkType
    public @interface FieldAccesses {
        FieldAccess[] value();
    }

    private static FieldAccess getFieldAccess(FieldAccess[] annotations) {
        for (var annotation : annotations) {
            if (checkConditions(annotation.conditions())) {
                return annotation;
            }
        }
        return null;
    }

    private sealed interface Processor permits FieldProcessor, ExecutableProcessor {
        void apply(ClassBuilder builder);
    }

    private record FieldProcessor(FieldAccessKind kind, FieldId target,
                                  String sketch_name, ProtoId sketch_proto) implements Processor {
        static FieldProcessor of(Method sketch, ClassLoader loader, FieldAccess annotation,
                                 Map<Class<?>, Field[]> cached_fields) {
            var kind = annotation.kind();
            var target_class = ClassUtils.forName(annotation.klass(), loader);
            var target = searchField(cached_fields.computeIfAbsent(
                    target_class, Reflection::getHiddenFields), annotation.name());
            makeFieldPublic(target);
            if (kind == INSTANCE_SETTER || kind == STATIC_SETTER) {
                makeFieldNonFinal(target);
            }
            var field_shorty = TypeId.of(target.getType()).getShorty();
            var access_shorty = switch (kind) {
                case INSTANCE_GETTER -> field_shorty + "L";
                case STATIC_GETTER -> Character.toString(field_shorty);
                case INSTANCE_SETTER -> "VL" + field_shorty;
                case STATIC_SETTER -> "V" + field_shorty;
            };
            var sketch_proto = ProtoId.of(sketch);
            var sketch_shorty = sketch_proto.computeShorty();
            if (!access_shorty.equals(sketch_shorty)) {
                throw new IllegalArgumentException(String.format(
                        "Shorty mismatch for %s access on field '%s': expected %s but was %s",
                        kind, target, access_shorty, sketch_shorty));
            }
            return new FieldProcessor(kind, FieldId.of(target), sketch.getName(), sketch_proto);
        }

        @Override
        public void apply(ClassBuilder cb) {
            cb.withMethod(mb -> mb
                    .withFlags(ACC_PUBLIC | ACC_FINAL)
                    .withName(sketch_name)
                    .withProto(sketch_proto)
                    .withCode(/* wide return */ 2, ib -> {
                        ib.generate_lines();
                        switch (kind) {
                            case STATIC_GETTER -> {
                                ib.sget(ib.l(0), target);
                                ib.return_shorty(target.getType().getShorty(), ib.l(0));
                            }
                            case STATIC_SETTER -> {
                                ib.sput(ib.p(0), target);
                                ib.return_void();
                            }
                            case INSTANCE_GETTER -> {
                                ib.iget(ib.l(0), ib.p(0), target);
                                ib.return_shorty(target.getType().getShorty(), ib.l(0));
                            }
                            case INSTANCE_SETTER -> {
                                ib.iput(ib.p(1), ib.p(0), target);
                                ib.return_void();
                            }
                        }
                    })
            );
        }
    }

    private record ExecutableProcessor(ExecutableAccessKind kind, MethodId target,
                                       String sketch_name,
                                       ProtoId sketch_proto) implements Processor {
        static ExecutableProcessor of(Method sketch, ClassLoader loader, ExecutableAccess annotation,
                                      Map<Class<?>, Executable[]> cached_executables) {
            var kind = annotation.kind();
            if ((kind == INVOKE_CONSTRUCTOR || kind == NEW_INSTANCE)
                    && !"<init>".equals(annotation.name())) {
                throw new IllegalArgumentException(String.format(
                        "Executable name must be '<init>' for %s access", kind));
            }

            Class<?> target_class = ClassUtils.forName(annotation.klass(), loader);
            var target = searchExecutable(cached_executables.computeIfAbsent(target_class,
                    Reflection::getHiddenExecutables), annotation.name(), annotation.args());
            makeExecutablePublic(target);

            var access_shorty = ProtoId.of(target).computeShorty();
            access_shorty = switch (kind) {
                case STATIC -> access_shorty;
                case VIRTUAL, INTERFACE, INVOKE_CONSTRUCTOR ->
                        new StringBuilder(access_shorty).insert(1, "L").toString();
                case NEW_INSTANCE -> "L" + access_shorty.substring(1);
            };
            var sketch_proto = ProtoId.of(sketch);
            var sketch_shorty = sketch_proto.computeShorty();
            if (!access_shorty.equals(sketch_shorty)) {
                throw new IllegalArgumentException(String.format(
                        "Shorty mismatch for %s access on method '%s': expected %s but was %s",
                        kind, target, access_shorty, sketch_shorty));
            }
            return new ExecutableProcessor(kind, MethodId.of(target), sketch.getName(), sketch_proto);
        }

        @Override
        public void apply(ClassBuilder cb) {
            cb.withMethod(mb -> mb
                    .withFlags(ACC_PUBLIC | ACC_FINAL)
                    .withName(sketch_name)
                    .withProto(sketch_proto)
                    .withCode(/* wide return */ 2, ib -> {
                        ib.generate_lines();
                        var ins = sketch_proto.countInputRegisters();
                        var ret_shorty = sketch_proto.getReturnType().getShorty();
                        switch (kind) {
                            case STATIC -> {
                                ib.invoke_range(STATIC, target, ins, ins == 0 ? 0 : ib.p(0));
                                ib.move_result_shorty(ret_shorty, ib.l(0));
                                ib.return_shorty(ret_shorty, ib.l(0));
                            }
                            case VIRTUAL -> {
                                ib.invoke_range(VIRTUAL, target, ins, ib.p(0));
                                ib.move_result_shorty(ret_shorty, ib.l(0));
                                ib.return_shorty(ret_shorty, ib.l(0));
                            }
                            case INTERFACE -> {
                                ib.invoke_range(INTERFACE, target, ins, ib.p(0));
                                ib.move_result_shorty(ret_shorty, ib.l(0));
                                ib.return_shorty(ret_shorty, ib.l(0));
                            }
                            case INVOKE_CONSTRUCTOR -> {
                                assert ret_shorty == 'V';
                                ib.invoke_range(DIRECT, target, ins, ib.p(0));
                                ib.return_void();
                            }
                            case NEW_INSTANCE -> {
                                assert ret_shorty == 'L';
                                ib.new_instance(ib.this_(), target.getDeclaringClass());
                                ib.invoke_range(DIRECT, target, ins + 1, ib.this_());
                                ib.return_object(ib.this_());
                            }
                        }
                    })
            );
        }
    }

    public static <T> Class<T> processSymbols(Class<T> clazz, ClassLoader loader, List<Processor> processors) {
        String access_name = Utils.generateClassName(loader, clazz.getName() + "$Access");
        TypeId access_id = TypeId.ofName(access_name);

        ClassDef access_def = ClassBuilder.build(access_id, cb -> {
            cb.withSuperClass(TypeId.of(clazz));
            cb.withFlags(ACC_PUBLIC);
            for (var proc : processors) {
                proc.apply(cb);
            }
        });

        DexFile dex = openDexFile(DexIO.write(Dex.of(access_def)));
        setTrusted(dex);

        var out = DexFileUtils.loadClass(dex, access_name, loader);
        ClassUtils.forceClassVerified(out);
        //noinspection unchecked
        return (Class<T>) out;
    }

    public static <T> Class<T> generateImplClass(Class<T> clazz) {
        return generateImplClass(clazz, clazz.getClassLoader());
    }

    public static <T> Class<T> generateImplClass(Class<T> clazz, ClassLoader loader) {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(loader);

        if ((clazz.getModifiers() & (ACC_INTERFACE | ACC_FINAL)) != 0) {
            throw new IllegalArgumentException(
                    "Interfaces and final classes are not allowed" + clazz);
        }

        // TODO: cached_classes?
        var cached_executables = new HashMap<Class<?>, Executable[]>();
        var cached_fields = new HashMap<Class<?>, Field[]>();

        Method[] sketch_methods = getDeclaredMethods(clazz);
        List<Processor> processors = new ArrayList<>(sketch_methods.length);

        for (Method method : sketch_methods) {
            var field_access = getFieldAccess(method.getDeclaredAnnotationsByType(FieldAccess.class));
            if (field_access != null) {
                processors.add(FieldProcessor.of(method, loader, field_access, cached_fields));
            } else {
                var executable_access = getExecutableAccess(method.getDeclaredAnnotationsByType(ExecutableAccess.class));
                if (executable_access != null) {
                    processors.add(ExecutableProcessor.of(method, loader, executable_access, cached_executables));
                } else {
                    // Skip, this method does not require processing
                    continue;
                }
            }
            if (!Modifier.isAbstract(method.getModifiers())) {
                throw new IllegalStateException("Method must be abstract " + method);
            }
        }

        return processSymbols(clazz, loader, processors);
    }

    public static <T> T generateImpl(Class<T> clazz) {
        return AndroidUnsafe.allocateInstance(generateImplClass(clazz));
    }

    public static <T> T generateImpl(Class<T> clazz, ClassLoader loader) {
        return AndroidUnsafe.allocateInstance(generateImplClass(clazz, loader));
    }
}
