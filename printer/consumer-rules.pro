# KmpPrinter Library ProGuard Rules
# These rules are automatically applied to any Android project that includes this library.

# Preserve the library's public API
-keep public class ngga.ring.printer.KmpPrinter { *; }
-keep public class ngga.ring.printer.ReceiptService { *; }
-keep public interface ngga.ring.printer.manager.** { *; }

# Preserve all models (important for serialization and state management)
-keep class ngga.ring.printer.model.** { *; }

# Keep specific Napier tags if used
-keep class io.github.aakira.napier.** { *; }

# Support for Coroutines (generic rules)
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.HandlerContext {
    private static java.util.concurrent.atomic.AtomicIntegerFieldUpdater ...;
}

# Preserve native methods if any (e.g., jSerialComm on JVM, though this is for Android)
-keepclasseswithmembernames class * {
    native <methods>;
}

# Preserve JNA classes if they leak into Android (unlikely but safe)
-dontwarn com.fazecast.jSerialComm.**
-dontwarn net.java.dev.jna.**
