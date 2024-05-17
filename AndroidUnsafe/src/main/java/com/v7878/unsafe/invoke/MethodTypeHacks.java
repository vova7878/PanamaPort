package com.v7878.unsafe.invoke;

import static com.v7878.dex.DexConstants.ACC_FINAL;
import static com.v7878.dex.DexConstants.ACC_PRIVATE;
import static com.v7878.dex.DexConstants.ACC_PUBLIC;
import static com.v7878.dex.bytecode.CodeBuilder.Op.GET_OBJECT;
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

import com.v7878.dex.ClassDef;
import com.v7878.dex.Dex;
import com.v7878.dex.EncodedField;
import com.v7878.dex.EncodedMethod;
import com.v7878.dex.FieldId;
import com.v7878.dex.MethodId;
import com.v7878.dex.ProtoId;
import com.v7878.dex.TypeId;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.ApiSensitive;
import com.v7878.unsafe.ClassUtils;

import java.lang.invoke.MethodType;
import java.util.Objects;

import dalvik.system.DexFile;

@ApiSensitive
public class MethodTypeHacks {

    public static final Class<?> INVOKE_FORM = nothrows_run(
            () -> Class.forName("java.lang.invoke.MethodTypeForm"));
    public static final Class<MethodTypeForm> FORM_IMPL;

    static {
        ClassUtils.makeClassPublicNonFinal(INVOKE_FORM);

        TypeId mtf = TypeId.of(MethodTypeForm.class);
        TypeId i_arr = TypeId.of(int[].class);

        String form_name = MethodTypeForm.class.getName() + "$MethodTypeFormImpl";
        TypeId form_id = TypeId.of(form_name);

        ClassDef form_def = new ClassDef(form_id);
        form_def.setSuperClass(TypeId.of(INVOKE_FORM));
        form_def.getInterfaces().add(mtf);
        form_def.setAccessFlags(ACC_PUBLIC | ACC_FINAL);

        FieldId fo_field = new FieldId(form_id, i_arr, "frameOffsets");
        form_def.getClassData().getInstanceFields().add(
                new EncodedField(fo_field, ACC_PRIVATE | ACC_FINAL, null)
        );

        FieldId ro_field = new FieldId(form_id, i_arr, "referencesOffsets");
        form_def.getClassData().getInstanceFields().add(
                new EncodedField(ro_field, ACC_PRIVATE | ACC_FINAL, null)
        );

        form_def.getClassData().getVirtualMethods().add(new EncodedMethod(
                new MethodId(form_id, new ProtoId(i_arr), "frameOffsets"),
                ACC_PUBLIC).withCode(1, b -> b
                .iop(GET_OBJECT, b.l(0), b.this_(), fo_field)
                .return_object(b.l(0))
        ));

        form_def.getClassData().getVirtualMethods().add(new EncodedMethod(
                new MethodId(form_id, new ProtoId(i_arr), "referencesOffsets"),
                ACC_PUBLIC).withCode(1, b -> b
                .iop(GET_OBJECT, b.l(0), b.this_(), ro_field)
                .return_object(b.l(0))
        ));

        DexFile dex = openDexFile(new Dex(form_def).compile());

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
                Class<?>[] ptypes = Transformers.ptypes(type);
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
