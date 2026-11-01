package ess.ll.client.render.targetfx.internal;

/** Only the string operations used by the five visual modules. */
public final class StringConcatSupport {
   private StringConcatSupport() {
   }

   public static String concat135(char a, char b, char c, char d, char e, char f, char g, char h) {
      return "" + a + b + c + d + e + f + g + h;
   }

   public static String concat136(String value) {
      return "Invalid color: " + value;
   }

   public static String concat470(String name) {
      return "fonts/" + name;
   }

   public static String concat471(String name) {
      return "images/" + name + ".png";
   }

   public static String concat472(String name) {
      return "core/" + name;
   }

   public static String concat95(String name) {
      return "pipeline/" + name;
   }

   public static String concat464(boolean a, boolean b, boolean c) {
      return "dimasik/nightix_cube_" + a + "_" + b + "_" + c;
   }
}
