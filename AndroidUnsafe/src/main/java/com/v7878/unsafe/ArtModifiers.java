package com.v7878.unsafe;

import static com.v7878.unsafe.ArtVersion.ART_SDK_INT;
import static com.v7878.unsafe.Utils.unsupportedSDK;

public class ArtModifiers {
    @ApiSensitive
    public static final int kAccSkipAccessChecks = switch (ART_SDK_INT) {
        case 36 /*android 16*/, 35 /*android 15*/, 34 /*android 14*/,
             33 /*android 13*/, 32 /*android 12L*/, 31 /*android 12*/,
             30 /*android 11*/, 29 /*android 10*/, 28 /*android 9*/,
             27 /*android 8.1*/, 26 /*android 8*/ -> 0x00080000;
        default -> throw unsupportedSDK(ART_SDK_INT);
    };

    @ApiSensitive
    public static final int kAccSingleImplementation = switch (ART_SDK_INT) {
        case 36 /*android 16*/, 35 /*android 15*/, 34 /*android 14*/,
             33 /*android 13*/, 32 /*android 12L*/, 31 /*android 12*/,
             30 /*android 11*/, 29 /*android 10*/, 28 /*android 9*/,
             27 /*android 8.1*/, 26 /*android 8*/ -> 0x08000000;
        default -> throw unsupportedSDK(ART_SDK_INT);
    };

    @ApiSensitive
    public static final int kAccIntrinsic = 0x80000000;

    @ApiSensitive
    public static final int kAccCompileDontBother = switch (ART_SDK_INT) {
        case 36 /*android 16*/, 35 /*android 15*/, 34 /*android 14*/, 33 /*android 13*/,
             32 /*android 12L*/, 31 /*android 12*/, 30 /*android 11*/, 29 /*android 10*/,
             28 /*android 9*/, 27 /*android 8.1*/ -> 0x02000000;
        case 26 /*android 8*/ -> 0x01000000;
        default -> throw unsupportedSDK(ART_SDK_INT);
    };

    @ApiSensitive
    public static final int kAccPreCompiled = switch (ART_SDK_INT) {
        case 36 /*android 16*/, 35 /*android 15*/, 34 /*android 14*/,
             33 /*android 13*/, 32 /*android 12L*/, 31 /*android 12*/ -> 0x00800000;
        case 30 /*android 11*/ -> 0x00200000;
        case 29 /*android 10*/, 28 /*android 9*/, 27 /*android 8.1*/, 26 /*android 8*/ -> 0;
        default -> throw unsupportedSDK(ART_SDK_INT);
    };

    @ApiSensitive
    public static final int kAccFastInterpreterToInterpreterInvoke = switch (ART_SDK_INT) {
        case 36 /*android 16*/, 35 /*android 15*/,
             34 /*android 14*/, 33 /*android 13*/ -> 0;
        case 32 /*android 12L*/, 31 /*android 12*/,
             30 /*android 11*/, 29 /*android 10*/ -> 0x40000000;
        case 28 /*android 9*/, 27 /*android 8.1*/, 26 /*android 8*/ -> 0;
        default -> throw unsupportedSDK(ART_SDK_INT);
    };

    @ApiSensitive
    public static final int kAccPublicApi = switch (ART_SDK_INT) {
        case 36 /*android 16*/, 35 /*android 15*/, 34 /*android 14*/,
             33 /*android 13*/, 32 /*android 12L*/, 31 /*android 12*/,
             30 /*android 11*/, 29 /*android 10*/ -> 0x10000000;
        case 28 /*android 9*/, 27 /*android 8.1*/, 26 /*android 8*/ -> 0;
        default -> throw unsupportedSDK(ART_SDK_INT);
    };

    @ApiSensitive
    public static final int kAccCorePlatformApi = switch (ART_SDK_INT) {
        case 36 /*android 16*/, 35 /*android 15*/, 34 /*android 14*/,
             33 /*android 13*/, 32 /*android 12L*/, 31 /*android 12*/,
             30 /*android 11*/, 29 /*android 10*/ -> 0x20000000;
        case 28 /*android 9*/, 27 /*android 8.1*/, 26 /*android 8*/ -> 0;
        default -> throw unsupportedSDK(ART_SDK_INT);
    };

    @ApiSensitive
    public static final int kAccHiddenapiBits = switch (ART_SDK_INT) {
        case 36 /*android 16*/, 35 /*android 15*/, 34 /*android 14*/,
             33 /*android 13*/, 32 /*android 12L*/, 31 /*android 12*/,
             30 /*android 11*/, 29 /*android 10*/, 28 /*android 9*/ -> 0x30000000;
        case 27 /*android 8.1*/, 26 /*android 8*/ -> 0;
        default -> throw unsupportedSDK(ART_SDK_INT);
    };

    static int makePublicApi(int flags) {
        if (ART_SDK_INT <= 27) {
            return flags;
        }
        flags &= ~kAccHiddenapiBits;
        if (ART_SDK_INT == 28) {
            return flags;
        }
        return flags | kAccPublicApi;
    }
}
