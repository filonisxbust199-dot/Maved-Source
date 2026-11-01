package ess.ll.client.render.targetfx.model;

public enum ThemeColorRole {
   MAIN("1 Цвет темы", "Main"),
   MAIN_TWO("2 Цвет темы", "Main"),
   OUTLINE("Цвет обводки", "Click gui"),
   BACKGROUND("Цвет заднего фона", "Click gui"),
   SHADOW("Цвет свечения", "Click gui"),
   LINE("Цвет разделителей", "Click gui"),
   SCROLL("Цвет линии скролла", "Click gui"),
   ACTIVE_STATE("Активный переключатель", "Click gui"),
   DEACTIVE_STATE("Не активный переключатель", "Click gui"),
   CIRCLE("Цвет слайдерного круга", "Click gui"),
   SELECTED("Цвет выбранного элемента", "Click gui"),
   ACTIVE_TEXT("Активный текст", "Click gui"),
   DEACTIVE_TEXT("Не активный текст", "Click gui"),
   OPEN_SETTING_TEXT("Цвет текста открытой настройки", "Click gui"),
   DEOPEN_SETTING_TEXT("Цвет текста закрытой настройки", "Click gui"),
   TEXT("Цвет текста", "Click gui"),
   SUR_TEXT("Цвет под-текста", "Click gui"),
   DESELECT_CATEGORY_TEXT("Цвет текста не выбранной категорий", "Click gui"),
   CATEGORY_TEXT("Цвет текста категорий", "Click gui"),
   CATEGORY_SURTEST("Цвет под-текста категорий", "Click gui"),
   WINDOW_TEXT("Текст окон", "Interface"),
   WINDOW_SUR_TEXT("Доп. текст окон", "Interface"),
   WINDOW_BACKGROUND("Задний фон окон", "Interface"),
   WINDOW_BACKGROUND_TWO("Второй задний фон окон", "Interface"),
   WINDOW_SHADOW("Свечение окон", "Interface");

   private final String displayName;
   private final String category;

   private ThemeColorRole(String text, String text2) {
      this.displayName = text;
      this.category = text2;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public String getCategory() {
      return this.category;
   }
}
