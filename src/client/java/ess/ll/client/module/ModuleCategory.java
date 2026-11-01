package ess.ll.client.module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum ModuleCategory {
   Combat("8", ModuleSubCategory.Combat, ModuleSubCategory.Utility, ModuleSubCategory.Other),
   Movement("1", ModuleSubCategory.Main, ModuleSubCategory.Other),
   Player("6", ModuleSubCategory.Main, ModuleSubCategory.Other),
   Render("4", ModuleSubCategory.World, ModuleSubCategory.Interface, ModuleSubCategory.Other),
   Misc("3", ModuleSubCategory.Servers, ModuleSubCategory.Utility, ModuleSubCategory.Other),
   AutoBuy("D"),
   Friends("7"),
   Configs("2"),
   Bots("K"),
   Themes("O"),
   Cosmetics("N");

   private final List<ModuleSubCategory> subCategories = new ArrayList<>();
   private final String icon;

   ModuleCategory(String text, ModuleSubCategory... subCategories) {
      this.icon = text;
      this.subCategories.addAll(Arrays.asList(subCategories));
   }

   public boolean hasSubCategories() {
      return !this.subCategories.isEmpty();
   }

   public boolean hasPanel() {
      return this.hasSubCategories() || this == Configs || this == Cosmetics;
   }

   public List<ModuleSubCategory> getSubCategories() {
      return this.subCategories;
   }

   public String getIcon() {
      return this.icon;
   }
}
