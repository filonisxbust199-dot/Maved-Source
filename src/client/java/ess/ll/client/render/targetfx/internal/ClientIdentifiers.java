package ess.ll.client.render.targetfx.internal;

import ess.ll.client.render.targetfx.internal.StringConcatSupport;
import net.minecraft.resources.Identifier;
public final class ClientIdentifiers {
   private static final String string2 = "dimasik";
   private static final String string3 = "dimasik";
   private static final String string4 = "dimasik";
   private static final String string = "This is a utility class and cannot be instantiated";

   public static Identifier getIdentifier(String text) {
      return Identifier.fromNamespaceAndPath(string3, StringConcatSupport.concat470(text));
   }

   private ClientIdentifiers() {
      throw new UnsupportedOperationException(string);
   }

   public static Identifier assetIdentifier(String text2) {
      return Identifier.fromNamespaceAndPath(string4, StringConcatSupport.concat471(text2));
   }

   public static Identifier clientResource(String text3) {
      return Identifier.fromNamespaceAndPath(string2, StringConcatSupport.concat472(text3));
   }
}
