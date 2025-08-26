package com.v7878.unsafe;

import static com.v7878.unsafe.ArtVersion.A10;
import static com.v7878.unsafe.ArtVersion.A11;
import static com.v7878.unsafe.ArtVersion.A12;
import static com.v7878.unsafe.ArtVersion.A13;
import static com.v7878.unsafe.ArtVersion.A14;
import static com.v7878.unsafe.ArtVersion.A15;
import static com.v7878.unsafe.ArtVersion.A16;
import static com.v7878.unsafe.ArtVersion.A16p1;
import static com.v7878.unsafe.ArtVersion.A8p0;
import static com.v7878.unsafe.ArtVersion.A8p1;
import static com.v7878.unsafe.ArtVersion.A9;
import static com.v7878.unsafe.ArtVersion.ART_INDEX;
import static com.v7878.unsafe.Utils.unsupportedART;

// TODO: Review after android 16 qpr 2 becomes stable
public class ArtModifiers {
    @ApiSensitive
    public static final int kAccSkipAccessChecks = switch (ART_INDEX) {
        case A16p1, A16, A15, A14, A13, A12, A11, A10, A9, A8p1, A8p0 -> 0x00080000;
        default -> throw unsupportedART(ART_INDEX);
    };

    @ApiSensitive
    public static final int kAccCopied = switch (ART_INDEX) {
        case A16p1, A16, A15, A14, A13, A12 -> 0x01000000;
        case A11, A10, A9, A8p1, A8p0 -> 0x00100000;
        default -> throw unsupportedART(ART_INDEX);
    };

    @ApiSensitive
    public static final int kAccSingleImplementation = switch (ART_INDEX) {
        case A16p1, A16, A15, A14, A13, A12, A11, A10, A9, A8p1, A8p0 -> 0x08000000;
        default -> throw unsupportedART(ART_INDEX);
    };

    @ApiSensitive
    public static final int kAccIntrinsic = 0x80000000;

    @ApiSensitive
    public static final int kAccCompileDontBother = switch (ART_INDEX) {
        case A16p1, A16, A15, A14, A13, A12, A11, A10, A9, A8p1 -> 0x02000000;
        case A8p0 -> 0x01000000;
        default -> throw unsupportedART(ART_INDEX);
    };

    @ApiSensitive
    public static final int kAccPreCompiled = switch (ART_INDEX) {
        case A16p1, A16, A15, A14, A13, A12 -> 0x00800000;
        case A11 -> 0x00200000;
        case A10, A9, A8p1, A8p0 -> 0;
        default -> throw unsupportedART(ART_INDEX);
    };

    @ApiSensitive
    public static final int kAccFastInterpreterToInterpreterInvoke = switch (ART_INDEX) {
        case A16p1, A16, A15, A14, A13, A9, A8p1, A8p0 -> 0;
        case A12, A11, A10 -> 0x40000000;
        default -> throw unsupportedART(ART_INDEX);
    };

    @ApiSensitive
    public static final int kAccPublicApi = switch (ART_INDEX) {
        case A16p1, A16, A15, A14, A13, A12, A11, A10 -> 0x10000000;
        case A9, A8p1, A8p0 -> 0;
        default -> throw unsupportedART(ART_INDEX);
    };

    @ApiSensitive
    public static final int kAccCorePlatformApi = switch (ART_INDEX) {
        case A16p1, A16, A15, A14, A13, A12, A11, A10 -> 0x20000000;
        case A9, A8p1, A8p0 -> 0;
        default -> throw unsupportedART(ART_INDEX);
    };

    @ApiSensitive
    public static final int kAccHiddenapiBits = switch (ART_INDEX) {
        case A16p1, A16, A15, A14, A13, A12, A11, A10, A9 -> 0x30000000;
        case A8p1, A8p0 -> 0;
        default -> throw unsupportedART(ART_INDEX);
    };

    static int makePublicApi(int flags) {
        if (ART_INDEX < A9) {
            return flags;
        }
        flags &= ~kAccHiddenapiBits;
        if (ART_INDEX == A9) {
            return flags;
        }
        return flags | kAccPublicApi;
    }
}
