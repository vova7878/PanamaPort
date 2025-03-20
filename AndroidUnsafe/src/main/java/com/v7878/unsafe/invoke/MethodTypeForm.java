package com.v7878.unsafe.invoke;

import static com.v7878.unsafe.invoke.EmulatedStackFrame.RETURN_VALUE_IDX;

import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.unsafe.DangerLevel;

import java.lang.invoke.MethodType;

@DoNotObfuscate
public interface MethodTypeForm {
    MethodType erasedType();

    MethodType basicType();

    int parameterCount();

    int parameterSlotCount();

    int returnCount();

    int returnSlotCount();

    int primitiveParameterCount();

    int longPrimitiveParameterCount();

    int primitiveReturnCount();

    int longPrimitiveReturnCount();

    boolean hasPrimitives();

    boolean hasNonVoidPrimitives();

    boolean hasLongPrimitives();

    int parameterToArgSlot(int i);

    int argSlotToParameter(int argSlot);

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    @DoNotShrink
    int[] primitivesOffsets();

    default int primitivesCount() {
        var prims = primitivesOffsets();
        return prims[prims.length - 1];
    }

    default int parameterToPrimitivesOffset(int i) {
        int[] offsets = primitivesOffsets();
        if (i == RETURN_VALUE_IDX) {
            return offsets[offsets.length - 1];
        }
        return offsets[i];
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    @DoNotShrink
    int[] referencesOffsets();

    default int referencesCount() {
        var refs = referencesOffsets();
        return refs[refs.length - 1];
    }

    default int parameterToReferencesOffset(int i) {
        int[] offsets = referencesOffsets();
        if (i == RETURN_VALUE_IDX) {
            return offsets[offsets.length - 1];
        }
        return offsets[i];
    }
}
