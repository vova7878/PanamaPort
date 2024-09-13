package com.v7878.unsafe.invoke;

import static com.v7878.unsafe.invoke.EmulatedStackFrame.RETURN_VALUE_IDX;

import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.DangerLevel;

import java.lang.invoke.MethodType;

@DoNotShrinkType
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
    int[] frameOffsets();

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    @DoNotShrink
    int[] referencesOffsets();

    default int parameterToFrameOffset(int i) {
        int[] offsets = frameOffsets();
        if (i == RETURN_VALUE_IDX) {
            return offsets[offsets.length - 1];
        }
        return offsets[i];
    }

    default int parameterToReferencesOffset(int i) {
        int[] offsets = referencesOffsets();
        if (i == RETURN_VALUE_IDX) {
            return offsets[offsets.length - 1];
        }
        return offsets[i];
    }
}
