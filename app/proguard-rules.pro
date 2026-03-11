# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preservar firmas genéricas para Gson
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# Reglas específicas para Gson
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type

# Preservar tus modelos para que Gson pueda usarlos (ajusta el paquete si es necesario)
-keep class com.alos895.simplepos.model.** { *; }
-keep class com.alos895.simplepos.db.entity.** { *; }

# Si usas Room, también es bueno mantener sus entidades
-keep class androidx.room.Entity
