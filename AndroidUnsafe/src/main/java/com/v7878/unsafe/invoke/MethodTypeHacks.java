package com.v7878.unsafe.invoke;

import static com.v7878.dex.DexConstants.ACC_FINAL;
import static com.v7878.dex.DexConstants.ACC_PRIVATE;
import static com.v7878.dex.DexConstants.ACC_PUBLIC;
import static com.v7878.dex.builder.CodeBuilder.Op.GET_OBJECT;
import static com.v7878.unsafe.AndroidUnsafe.allocateInstance;
import static com.v7878.unsafe.AndroidUnsafe.getLongO;
import static com.v7878.unsafe.AndroidUnsafe.getObject;
import static com.v7878.unsafe.AndroidUnsafe.putLongO;
import static com.v7878.unsafe.AndroidUnsafe.putObject;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.Reflection.fieldOffset;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.invoke.EmulatedStackFrame.getSize;

import com.v7878.dex.DexIO;
import com.v7878.dex.builder.ClassBuilder;
import com.v7878.dex.immutable.ClassDef;
import com.v7878.dex.immutable.Dex;
import com.v7878.dex.immutable.FieldId;
import com.v7878.dex.immutable.TypeId;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.ApiSensitive;
import com.v7878.unsafe.ClassUtils;
import com.v7878.unsafe.access.InvokeAccess;

import java.lang.invoke.MethodType;
import java.util.Objects;

import dalvik.system.DexFile;

@ApiSensitive
public class MethodTypeHacks {

    public static final Class<?> INVOKE_FORM = nothrows_run(
            () -> Class.forName("java.lang.invoke.MethodTypeForm"));
    public static final Class<MethodTypeForm> FORM_IMPL;

    static {
        ClassUtils.makeClassInheritable(INVOKE_FORM);

        TypeId mtf = TypeId.of(MethodTypeForm.class);
        TypeId i_arr = TypeId.of(int[].class);

        String form_name = MethodTypeForm.class.getName() + "$MethodTypeFormImpl";
        TypeId form_id = TypeId.ofName(form_name);

        FieldId fo_field = FieldId.of(form_id, "frameOffsets", i_arr);
        FieldId ro_field = FieldId.of(form_id, "referencesOffsets", i_arr);

        ClassDef form_def = ClassBuilder.build(form_id, cb -> cb
                .withSuperClass(TypeId.of(INVOKE_FORM))
                .withInterfaces(mtf)
                .withFlags(ACC_PUBLIC | ACC_FINAL)
                .withField(fb -> fb
                        .of(fo_field)
                        .withFlags(ACC_PRIVATE | ACC_FINAL)
                )
                .withField(fb -> fb
                        .of(ro_field)
                        .withFlags(ACC_PRIVATE | ACC_FINAL)
                )
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC)
                        .withName("frameOffsets")
                        .withReturnType(i_arr)
                        .withParameters()
                        .withCode(1, ib -> ib
                                .iop(GET_OBJECT, ib.l(0), ib.this_(), fo_field)
                                .return_object(ib.l(0))
                        )
                )
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC)
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

        ClassLoader loader = MethodTypeForm.class.getClassLoader();

        //noinspection unchecked
        FORM_IMPL = (Class<MethodTypeForm>) loadClass(dex, form_name, loader);
    }

    public static final int ARG_TO_SLOT_TABLE_OFFSET = fieldOffset(getDeclaredField(INVOKE_FORM, "argToSlotTable"));
    public static final int SLOT_TO_ARG_TABLE_OFFSET = fieldOffset(getDeclaredField(INVOKE_FORM, "slotToArgTable"));
    public static final int ARG_COUNTS_OFFSET = fieldOffset(getDeclaredField(INVOKE_FORM, "argCounts"));
    public static final int PRIM_COUNTS_OFFSET = fieldOffset(getDeclaredField(INVOKE_FORM, "primCounts"));
    public static final int ERASED_TYPE_OFFSET = fieldOffset(getDeclaredField(INVOKE_FORM, "erasedType"));
    public static final int BASIC_TYPE_OFFSET = fieldOffset(getDeclaredField(INVOKE_FORM, "basicType"));

    private static void copyFormData(Object from, Object to) {
        putObject(to, ARG_TO_SLOT_TABLE_OFFSET, getObject(from, ARG_TO_SLOT_TABLE_OFFSET));
        putObject(to, SLOT_TO_ARG_TABLE_OFFSET, getObject(from, SLOT_TO_ARG_TABLE_OFFSET));
        putLongO(to, ARG_COUNTS_OFFSET, getLongO(from, ARG_COUNTS_OFFSET));
        putLongO(to, PRIM_COUNTS_OFFSET, getLongO(from, PRIM_COUNTS_OFFSET));
        putObject(to, ERASED_TYPE_OFFSET, getObject(from, ERASED_TYPE_OFFSET));
        putObject(to, BASIC_TYPE_OFFSET, getObject(from, BASIC_TYPE_OFFSET));
    }

    public static final int FORM_OFFSET = fieldOffset(getDeclaredField(MethodType.class, "form"));
    public static final int FO_OFFSET = fieldOffset(getDeclaredField(FORM_IMPL, "frameOffsets"));
    public static final int RO_OFFSET = fieldOffset(getDeclaredField(FORM_IMPL, "referencesOffsets"));

    public static MethodTypeForm getForm(MethodType type) {
        Objects.requireNonNull(type);
        Object old_form = AndroidUnsafe.getObject(type, FORM_OFFSET);
        if (old_form instanceof MethodTypeForm mtf) {
            return mtf;
        }

        MethodType erased = type.erase();
        MethodTypeForm new_form;
        if (type != erased) {
            new_form = getForm(erased);
        } else {
            new_form = allocateInstance(FORM_IMPL);
            copyFormData(old_form, new_form);

            {
                Class<?>[] ptypes = InvokeAccess.ptypes(type);
                int[] frameOffsets = new int[ptypes.length + 1];
                int[] referencesOffsets = new int[ptypes.length + 1];
                int frameOffset = 0;
                int referenceOffset = 0;
                for (int i = 0; i < ptypes.length; i++) {
                    Class<?> ptype = ptypes[i];
                    if (ptype.isPrimitive()) {
                        frameOffset += getSize(ptype);
                    } else {
                        referenceOffset++;
                    }
                    frameOffsets[i + 1] = frameOffset;
                    referencesOffsets[i + 1] = referenceOffset;
                }
                AndroidUnsafe.putObject(new_form, FO_OFFSET, frameOffsets);
                AndroidUnsafe.putObject(new_form, RO_OFFSET, referencesOffsets);
            }
        }

        AndroidUnsafe.putObject(type, FORM_OFFSET, new_form);
        return new_form;
    }
}
