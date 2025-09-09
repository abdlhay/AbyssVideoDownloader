-ignorewarnings


-keep public class com.abmo.MainKt { public static void main(java.lang.String[]); }
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }


-keep public class com.abmo.model.** { *; }

# obfuscate everything else
-dontnote
-dontoptimize
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations, Signature


#------------------org.rhino-------------------
# Keep core Rhino classes (including contexts, scriptable objects, and execution-related classes)
-keep class org.mozilla.javascript.** { *; }

# Keep Rhino's access to Java objects (prevents issues when embedding Java in JS)
-keepclassmembers class org.mozilla.javascript.** {
    public *;
}
# Keep all methods and fields accessed reflectively (from JavaScript code)
-keepclassmembers class * {
    @org.mozilla.javascript.annotations.JSFunction public *;
    @org.mozilla.javascript.annotations.JSGetter public *;
    @org.mozilla.javascript.annotations.JSSetter public *;
}

# Keep Rhino-specific annotations used for JavaScript interoperation
-keepattributes RuntimeVisibleAnnotations

# Keep all JSFunction, JSGetter, JSSetter annotated methods
-keep @org.mozilla.javascript.annotations.JSFunction class * { *; }
-keep @org.mozilla.javascript.annotations.JSGetter class * { *; }
-keep @org.mozilla.javascript.annotations.JSSetter class * { *; }

# Keep native JavaScript-to-Java wrappers (if your code uses Java objects in JavaScript)
-keep class * extends org.mozilla.javascript.Scriptable { *; }
#------------------org.rhino-------------------


#------------------org.apache-------------------
# Keep all classes in the HttpComponents library
-keep class org.apache.http.** { *; }

# Keep everything in org.apache.http used for logging
-keep class org.apache.commons.logging.** { *; }

# Keep class and method names required for reflection or serialization in HttpComponents
-keepattributes Signature, RuntimeVisibleAnnotations, AnnotationDefault

# Allow obfuscation, but prevent stripping of method parameters (optional)
-keepclassmembers class * {
    public void set*(***);
    public *** get*();
}
#------------------org.apache-------------------


#------------------org.jsoup-------------------
# Keep all Jsoup classes and methods (Jsoup uses reflection)
-keep class org.jsoup.** { *; }

# Keep methods that are used reflectively for data extraction and parsing
-keepclassmembers class org.jsoup.nodes.** {
    *;
}
#------------------org.jsoup-------------------


-keep class org.bouncycastle.** { *; }

-keep class org.slf4j.** { *; }