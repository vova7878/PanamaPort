package com.v7878.unsafe.invoke;

import static com.v7878.dex.DexConstants.ACC_FINAL;
import static com.v7878.dex.DexConstants.ACC_PRIVATE;
import static com.v7878.dex.DexConstants.ACC_PUBLIC;
import static com.v7878.dex.builder.CodeBuilder.Op.GET;
import static com.v7878.dex.builder.CodeBuilder.Op.GET_BOOLEAN;
import static com.v7878.dex.builder.CodeBuilder.Op.GET_BYTE;
import static com.v7878.dex.builder.CodeBuilder.Op.GET_CHAR;
import static com.v7878.dex.builder.CodeBuilder.Op.GET_OBJECT;
import static com.v7878.dex.builder.CodeBuilder.Op.GET_SHORT;
import static com.v7878.dex.builder.CodeBuilder.Op.GET_WIDE;
import static com.v7878.dex.builder.CodeBuilder.Op.PUT;
import static com.v7878.dex.builder.CodeBuilder.Op.PUT_BOOLEAN;
import static com.v7878.dex.builder.CodeBuilder.Op.PUT_BYTE;
import static com.v7878.dex.builder.CodeBuilder.Op.PUT_CHAR;
import static com.v7878.dex.builder.CodeBuilder.Op.PUT_OBJECT;
import static com.v7878.dex.builder.CodeBuilder.Op.PUT_SHORT;
import static com.v7878.dex.builder.CodeBuilder.Op.PUT_WIDE;
import static com.v7878.dex.util.ShortyUtils.unrecognizedShorty;
import static com.v7878.unsafe.AndroidUnsafe.allocateInstance;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.Reflection.fieldOffset;
import static com.v7878.unsafe.Reflection.getHiddenInstanceField;
import static com.v7878.unsafe.Reflection.getHiddenInstanceFields;
import static com.v7878.unsafe.Utils.DEBUG_BUILD;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.invoke.EmulatedStackFrame.getSize;

import com.v7878.dex.DexIO;
import com.v7878.dex.builder.ClassBuilder;
import com.v7878.dex.builder.CodeBuilder;
import com.v7878.dex.immutable.ClassDef;
import com.v7878.dex.immutable.Dex;
import com.v7878.dex.immutable.FieldId;
import com.v7878.dex.immutable.TypeId;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.ApiSensitive;
import com.v7878.unsafe.ArtFieldUtils;
import com.v7878.unsafe.ClassUtils;
import com.v7878.unsafe.ClassUtils.ClassStatus;
import com.v7878.unsafe.DexFileUtils;
import com.v7878.unsafe.access.InvokeAccess;

import java.lang.invoke.MethodType;
import java.util.Objects;

import dalvik.system.DexFile;

@ApiSensitive
public class MethodTypeHacks {
    public static final Class<?> INVOKE_FORM = nothrows_run(
            () -> Class.forName("java.lang.invoke.MethodTypeForm"));
    public static final Class<MethodTypeForm0> FORM_IMPL;

    @DoNotShrinkType
    private interface MethodTypeForm0 extends MethodTypeForm {
        @DoNotShrink
        @DoNotObfuscate
        void init(Object form, int[] primitivesOffsets, int[] referencesOffsets);
    }

    // TODO: move to CodeBuilder
    private static void iget(CodeBuilder ib, int value_reg_or_pair, int object_reg, FieldId instance_field) {
        var shorty = instance_field.getType().getShorty();
        var op = switch (shorty) {
            case 'Z' -> GET_BOOLEAN;
            case 'B' -> GET_BYTE;
            case 'S' -> GET_SHORT;
            case 'C' -> GET_CHAR;
            case 'I', 'F' -> GET;
            case 'J', 'D' -> GET_WIDE;
            case 'L' -> GET_OBJECT;
            default -> throw unrecognizedShorty(shorty);
        };
        ib.iop(op, value_reg_or_pair, object_reg, instance_field);
    }

    // TODO: move to CodeBuilder
    private static void iput(CodeBuilder ib, int value_reg_or_pair, int object_reg, FieldId instance_field) {
        var shorty = instance_field.getType().getShorty();
        var op = switch (shorty) {
            case 'Z' -> PUT_BOOLEAN;
            case 'B' -> PUT_BYTE;
            case 'S' -> PUT_SHORT;
            case 'C' -> PUT_CHAR;
            case 'I', 'F' -> PUT;
            case 'J', 'D' -> PUT_WIDE;
            case 'L' -> PUT_OBJECT;
            default -> throw unrecognizedShorty(shorty);
        };
        ib.iop(op, value_reg_or_pair, object_reg, instance_field);
    }

    static {
        ClassUtils.makeClassInheritable(INVOKE_FORM);

        TypeId mtf = TypeId.of(MethodTypeForm0.class);
        TypeId i_arr = TypeId.of(int[].class);

        String form_name = MethodTypeForm.class.getName() + "$Impl";
        TypeId form_id = TypeId.ofName(form_name);

        FieldId po_field = FieldId.of(form_id, "primitivesOffsets", i_arr);
        FieldId ro_field = FieldId.of(form_id, "referencesOffsets", i_arr);

        ClassDef form_def = ClassBuilder.build(form_id, cb -> cb
                .withSuperClass(TypeId.of(INVOKE_FORM))
                .withInterfaces(mtf)
                .withFlags(ACC_PUBLIC | ACC_FINAL)
                .withField(fb -> fb
                        .of(po_field)
                        .withFlags(ACC_PRIVATE)
                )
                .withField(fb -> fb
                        .of(ro_field)
                        .withFlags(ACC_PRIVATE)
                )
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC | ACC_FINAL)
                        .withName("init")
                        .withReturnType(TypeId.V)
                        .withParameterTypes(TypeId.OBJECT, TypeId.I.array(), TypeId.I.array())
                        .withCode(2, ib -> {
                            if (DEBUG_BUILD) {
                                ib.check_cast(ib.p(0), TypeId.of(INVOKE_FORM));
                            }
                            ib.iop(PUT_OBJECT, ib.p(1), ib.this_(), po_field);
                            ib.iop(PUT_OBJECT, ib.p(2), ib.this_(), ro_field);
                            for (var field : getHiddenInstanceFields(INVOKE_FORM)) {
                                ArtFieldUtils.makeFieldPublic(field);
                                ArtFieldUtils.makeFieldNonFinal(field);

                                var field_id = FieldId.of(field);
                                iget(ib, ib.l(0), ib.p(0), field_id);
                                iput(ib, ib.l(0), ib.this_(), field_id);
                            }
                            ib.return_void();
                        })
                )
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC | ACC_FINAL)
                        .withName("primitivesOffsets")
                        .withReturnType(i_arr)
                        .withParameters()
                        .withCode(1, ib -> ib
                                .iop(GET_OBJECT, ib.l(0), ib.this_(), po_field)
                                .return_object(ib.l(0))
                        )
                )
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC | ACC_FINAL)
                        .withName("referencesOffsets")
                        .withReturnType(i_arr)
                        .withParameters()
                        .withCode(1, ib -> ib
                                .iop(GET_OBJECT, ib.l(0), ib.this_(), ro_field)
                                .return_object(ib.l(0))
                        )
                )
        );

        DexFile dex = openDexFile(DexIO.write(Dex.of(form_def)));
        DexFileUtils.setTrusted(dex);

        ClassLoader loader = MethodTypeForm0.class.getClassLoader();

        //noinspection unchecked
        FORM_IMPL = (Class<MethodTypeForm0>) loadClass(dex, form_name, loader);
        if (!DEBUG_BUILD) {
            ClassUtils.setClassStatus(FORM_IMPL, ClassStatus.Verified);
        }
    }

    public static final int FORM_OFFSET = fieldOffset(
            getHiddenInstanceField(MethodType.class, "form"));

    private static MethodTypeForm0 getForm0(MethodType type) {
        Objects.requireNonNull(type);
        Object old_form = AndroidUnsafe.getObject(type, FORM_OFFSET);
        if (old_form instanceof MethodTypeForm0 mtf) {
            return mtf;
        }

        MethodType erased = type.erase();
        MethodTypeForm0 new_form;
        if (type != erased) {
            new_form = getForm0(erased);
        } else {
            new_form = allocateInstance(FORM_IMPL);

            Class<?>[] ptypes = InvokeAccess.ptypes(type);
            Class<?> rtype = InvokeAccess.rtype(type);
            int[] primitivesOffsets = new int[ptypes.length + 2];
            int[] referencesOffsets = new int[ptypes.length + 2];

            int i = 0;
            int frameOffset = 0;
            int referenceOffset = 0;

            for (; i < ptypes.length; i++) {
                Class<?> ptype = ptypes[i];
                if (ptype.isPrimitive()) {
                    frameOffset += getSize(ptype);
                } else {
                    referenceOffset++;
                }
                primitivesOffsets[i + 1] = frameOffset;
                referencesOffsets[i + 1] = referenceOffset;
            }

            if (rtype.isPrimitive()) {
                frameOffset += getSize(rtype);
            } else {
                referenceOffset++;
            }
            primitivesOffsets[i + 1] = frameOffset;
            referencesOffsets[i + 1] = referenceOffset;

            new_form.init(old_form, primitivesOffsets, referencesOffsets);
        }

        AndroidUnsafe.putObject(type, FORM_OFFSET, new_form);
        return new_form;
    }

    public static MethodTypeForm getForm(MethodType type) {
        return getForm0(type);
    }
}
