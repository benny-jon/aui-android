# Preserve generated kotlinx-serialization accessors for the public AUI model
# surface when host apps shrink their own release builds. The library parser
# references serializers directly, but consumer apps are also expected to pass
# AUI models through Json encode/decode APIs from their own code.
-keepclassmembers class com.bennyjon.aui.core.model.** {
    *** Companion;
}

-keepclassmembers class com.bennyjon.aui.core.model.data.** {
    *** Companion;
}

-keepclassmembers class com.bennyjon.aui.core.model.**$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

-keepclassmembers class com.bennyjon.aui.core.model.data.**$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.bennyjon.aui.core.model.**$$serializer { *; }
-keep,includedescriptorclasses class com.bennyjon.aui.core.model.data.**$$serializer { *; }
