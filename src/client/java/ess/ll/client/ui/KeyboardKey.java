package ess.ll.client.ui;

import ess.ll.client.integration.MinecraftAccess;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum KeyboardKey implements MinecraftAccess {
   KEY_SPACE("Space", 32),
   KEY_APOSTROPHE("Apostrophe", 39),
   KEY_COMMA("Comma", 44),
   KEY_MINUS("Minus", 45),
   KEY_PERIOD("Period", 46),
   KEY_SLASH("Slash", 47),
   KEY_0("0", 48),
   KEY_1("1", 49),
   KEY_2("2", 50),
   KEY_3("3", 51),
   KEY_4("4", 52),
   KEY_5("5", 53),
   KEY_6("6", 54),
   KEY_7("7", 55),
   KEY_8("8", 56),
   KEY_9("9", 57),
   KEY_SEMICOLON("Semicolon", 59),
   KEY_EQUAL("Equals", 61),
   KEY_A("A", 65),
   KEY_B("B", 66),
   KEY_C("C", 67),
   KEY_D("D", 68),
   KEY_E("E", 69),
   KEY_F("F", 70),
   KEY_G("G", 71),
   KEY_H("H", 72),
   KEY_I("I", 73),
   KEY_J("J", 74),
   KEY_K("K", 75),
   KEY_L("L", 76),
   KEY_M("M", 77),
   KEY_N("N", 78),
   KEY_O("O", 79),
   KEY_P("P", 80),
   KEY_Q("Q", 81),
   KEY_R("R", 82),
   KEY_S("S", 83),
   KEY_T("T", 84),
   KEY_U("U", 85),
   KEY_V("V", 86),
   KEY_W("W", 87),
   KEY_X("X", 88),
   KEY_Y("Y", 89),
   KEY_Z("Z", 90),
   KEY_LEFT_BRACKET("LBRacket", 91),
   KEY_BACKSLASH("Backslash", 92),
   KEY_RIGHT_BRACKET("RBRacket", 93),
   KEY_GRAVE("Grave", 96),
   KEY_ESCAPE("Esc", 256),
   KEY_ENTER("Enter", 257),
   KEY_TAB("Tab", 258),
   KEY_BACKSPACE("Backspace", 259),
   KEY_INSERT("Ins", 260),
   KEY_DELETE("Delete", 261),
   KEY_RIGHT("Right", 262),
   KEY_LEFT("Left", 263),
   KEY_DOWN("Down", 264),
   KEY_UP("Up", 265),
   KEY_PAGE_UP("PgUp", 266),
   KEY_PAGE_DOWN("PgDn", 267),
   KEY_HOME("Home", 268),
   KEY_END("End", 269),
   KEY_CAPS_LOCK("CapsLock", 280),
   KEY_SCROLL_LOCK("ScrollLock", 281),
   KEY_NUM_LOCK("NumLock", 282),
   KEY_PRINT_SCREEN("PrtScr", 283),
   KEY_PAUSE("Pause", 284),
   KEY_F1("F1", 290),
   KEY_F2("F2", 291),
   KEY_F3("F3", 292),
   KEY_F4("F4", 293),
   KEY_F5("F5", 294),
   KEY_F6("F6", 295),
   KEY_F7("F7", 296),
   KEY_F8("F8", 297),
   KEY_F9("F9", 298),
   KEY_F10("F10", 299),
   KEY_F11("F11", 300),
   KEY_F12("F12", 301),
   KEY_KP_0("NumPad0", 320),
   KEY_KP_1("NumPad1", 321),
   KEY_KP_2("NumPad2", 322),
   KEY_KP_3("NumPad3", 323),
   KEY_KP_4("NumPad4", 324),
   KEY_KP_5("NumPad5", 325),
   KEY_KP_6("NumPad6", 326),
   KEY_KP_7("NumPad7", 327),
   KEY_KP_8("NumPad8", 328),
   KEY_KP_9("NumPad9", 329),
   KEY_KP_DECIMAL("KPDecimal", 330),
   KEY_KP_DIVIDE("KPDivide", 331),
   KEY_KP_MULTIPLY("KPMultiply", 332),
   KEY_KP_SUBTRACT("KPSubtract", 333),
   KEY_KP_ADD("KPAdd", 334),
   KEY_KP_ENTER("KPEnter", 335),
   KEY_KP_EQUAL("KPEqual", 336),
   KEY_LEFT_SHIFT("LShift", 340),
   KEY_LEFT_CONTROL("LCtrl", 341),
   KEY_LEFT_ALT("LAlt", 342),
   KEY_LEFT_SUPER("LSuper", 343),
   KEY_RIGHT_SHIFT("RShift", 344),
   KEY_RIGHT_CONTROL("RCtrl", 345),
   KEY_RIGHT_ALT("RAlt", 346),
   KEY_RIGHT_SUPER("RSuper", 347),
   KEY_MENU("Menu", 348),
   KEY_LAST("Last", 348),
   Mouse_1("Mouse1", -100),
   Mouse_2("Mouse2", -99),
   Mouse_3("Mouse3", -98),
   Mouse_4("Mouse4", -97),
   Mouse_5("Mouse5", -96),
   Mouse_6("Mouse6", -95),
   Mouse_7("Mouse7", -94),
   Mouse_8("Mouse8", -93),
   Mouse_LAST("MouseLAST", -93),
   Mouse_LEFT("LCM", -100),
   Mouse_RIGHT("RCM", -99),
   Mouse_MIDDLE("CKM", -98),
   KEY_NONE("NONE", -1);

   public final String name;
   public final int code;
   private static final Map<Integer, String> NAMES_BY_CODE = new HashMap<>();
   private static final Map<String, Integer> CODES_BY_NAME = new HashMap<>();

   @Override
   public String toString() {
      return this.name;
   }

   public static String nameOf(int code) {
      return NAMES_BY_CODE.getOrDefault(code, KEY_NONE.name);
   }

   public static int codeOf(String text) {
      return text == null ? KEY_NONE.code : CODES_BY_NAME.getOrDefault(text.toLowerCase(Locale.ROOT), KEY_NONE.code);
   }

   public static boolean isKeyDown(int code) {
      return !nameOf(code).toLowerCase(Locale.ROOT).contains("mouse") ? ess.ll.client.command.Keys.isDown(code) : false;
   }

   public static boolean isMouseDown(int code) {
      return nameOf(code).toLowerCase(Locale.ROOT).contains("mouse") ? ess.ll.client.command.Keys.isDown(code) : false;
   }

   public String getName() {
      return this.name;
   }

   public int getCode() {
      return this.code;
   }

   KeyboardKey(String text, int code) {
      this.name = text;
      this.code = code;
   }

   static {
      for (KeyboardKey keyboardKey : values()) {
         NAMES_BY_CODE.put(keyboardKey.code, keyboardKey.name);
         CODES_BY_NAME.put(keyboardKey.name.toLowerCase(Locale.ROOT), keyboardKey.code);
      }
   }
}
