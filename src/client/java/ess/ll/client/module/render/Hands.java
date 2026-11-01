package ess.ll.client.module.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import ess.ll.client.module.ClientModules;
import ess.ll.client.render.hands.HandsCompositeRenderer;
import ess.ll.client.render.targetfx.ClickGuiRenderer;
import ess.ll.client.render.targetfx.model.ThemeColorRole;
import ess.ll.client.setting.Settings.*;
import net.minecraft.client.Minecraft;
import org.joml.Vector4f;

/**
 * Hands, ported from dimasik: first-person hands and items are drawn into an offscreen target
 * (see HandsCaptureMixin) and composited back with a fill, shader, mirror, glow, outline or trail effect.
 */
public final class Hands {
   public static final Hands INSTANCE = new Hands();
   public final Setting[] settings;
   private boolean wasEnabled;
   public static volatile boolean stateActive;
   public static RenderTarget renderTarget;
   private final ModeSettings modeSetting;
   private final BooleanSetting autoColorPodTemuSetting;
   private final BooleanSetting autoColorPodPredmetSetting;
   private final SliderSetting smeshivanieSetting;
   private final SliderSetting iskazhenieZerkalaSetting;
   private final SliderSetting strengthRazmytiyaSetting;
   private final BooleanSetting raduzhnayaSetting;
   private final SliderSetting speedRadugiSetting;
   private final SliderSetting masshtabRadugiSetting;
   private final ColorSetting colorZalivkiSetting;
   private final SliderSetting opacityZalivkiSetting;
   private final BooleanSetting sohranitTeniSetting;
   private final SliderSetting strengthTeneySetting;
   private final ModeSettings stilSheyderaSetting;
   private final ColorSetting colorSheydera1Setting;
   private final BooleanSetting vtoroyColorSetting;
   private final ColorSetting colorSheydera2Setting;
   private final SliderSetting speedSheyderaSetting;
   private final SliderSetting masshtabSheyderaSetting;
   private final SliderSetting svechenieSheyderaSetting;
   private final SliderSetting kachestvoSheyderaSetting;
   private final BooleanSetting zvyozdySetting;
   private final SliderSetting opacitySheyderaSetting;
   private final BooleanSetting sohranitTeniSheyderaSetting;
   private final SliderSetting strengthTeneySheyderaSetting;
   private final BooleanSetting glovSetting;
   private final SliderSetting razmytieSetting;
   private final SliderSetting brightnessSetting;
   private final ColorSetting colorGlova1Setting;
   private final ColorSetting colorGlova2Setting;
   private final SliderSetting vnutrenneeSvechenieSetting;
   private final BooleanSetting obvodkaSetting;
   private final ColorSetting colorObvodkiSetting;
   private final SliderSetting tolshchinaObvodkiSetting;
   private final BooleanSetting shleyfSetting;
   private final ModeSettings modeShleyfaSetting;
   private final SliderSetting dlinaEnergiiSetting;
   private final SliderSetting brightnessEnergiiSetting;
   private final SliderSetting movementEnergiiSetting;
   private final SliderSetting pulsatsiyaEnergiiSetting;
   private final SliderSetting shleyfZaKameroySetting;
   private final SliderSetting dlinaLentSetting;
   private final SliderSetting widthLentSetting;
   private final SliderSetting brightnessLentSetting;
   private final SliderSetting strengthLentZaKameroySetting;
   private final SliderSetting podyomLentSetting;
   private final SliderSetting izgibLentSetting;
   private final SliderSetting speedVolnSetting;
   private final BooleanSetting lentyPriUdareSetting;
   private final SliderSetting dlinaPriUdareSetting;
   private final SliderSetting strengthPriUdareSetting;
   private final BooleanSetting lentyPriEdeSetting;
   private final SliderSetting dlinaPriEdeSetting;
   private final SliderSetting strengthPriEdeSetting;
   private final SliderSetting speedZatuhaniyaSetting;
   private final SliderSetting podyomSetting;
   private final SliderSetting kachanieSetting;
   private final SliderSetting shleyfOtKamerySetting;
   private final SliderSetting turbulentnostSetting;
   private final SliderSetting mertsanieSetting;
   private final BooleanSetting sduvPriUdareSetting;
   private final SliderSetting strengthSduvaSetting;
   private TextureTarget textureTarget;
   private boolean stateActive2;
   private long lastUpdateMillis;

   public float calculateValue15() {
      return this.speedRadugiSetting.get();
   }

   public int calculateValue23() {
      int value = Math.round(this.kachestvoSheyderaSetting.get());
      return value == 1 ? 3 : (value == 3 ? 8 : 5);
   }

   public int calculateValue40() {
      return this.autoColorPodTemuSetting.get()
         ? ClickGuiRenderer.getThemeColor(ThemeColorRole.MAIN)
         : this.colorSheydera1Setting.get();
   }

   public float calculateValue10() {
      return this.svechenieSheyderaSetting.get();
   }

   public float calculateValue17() {
      return this.pulsatsiyaEnergiiSetting.get();
   }

   public float calculateValue33() {
      return this.dlinaLentSetting.get();
   }

   public float calculateValue2() {
      return this.smeshivanieSetting.get();
   }

   public void updateState() {
      if (this.checkCondition13() && !this.stateActive2 && Minecraft.getInstance().player != null && Minecraft.getInstance().level != null) {
         RenderTarget renderTarget2 = Minecraft.getInstance().gameRenderer.mainRenderTarget();
         int value2 = Math.max(1, renderTarget2.width);
         int value3 = Math.max(1, renderTarget2.height);
         if (this.textureTarget == null) {
            this.textureTarget = new TextureTarget("hands-capture", value2, value3, GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT);
         } else if (this.textureTarget.width != value2 || this.textureTarget.height != value3) {
            this.textureTarget.resize(value2, value3);
         }

         RenderSystem.getDevice()
            .createCommandEncoder()
            .clearColorAndDepthTextures(this.textureTarget.getColorTexture(), new Vector4f(0.0F), this.textureTarget.getDepthTexture(), 0.0);
         renderTarget = this.textureTarget;
         stateActive = true;
         this.stateActive2 = true;
      }
   }

   public float calculateValue26() {
      return this.brightnessSetting.get();
   }

   public float calculateValue16() {
      return this.dlinaPriUdareSetting.get();
   }

   public int calculateValue44() {
      if (this.checkCondition19()) {
         return 4;
      } else {
         return this.checkCondition2()
            ? 0
            : (
               this.modeSetting.is("Шейдер")
                  ? 1
                  : (this.modeSetting.is("Зеркало") ? 2 : 3)
            );
      }
   }

   public int calculateValue31() {
      if (this.autoColorPodTemuSetting.get()) {
         return this.vtoroyColorSetting.get()
            ? ClickGuiRenderer.getThemeColor(ThemeColorRole.MAIN_TWO)
            : ClickGuiRenderer.getThemeColor(ThemeColorRole.MAIN);
      } else {
         return this.vtoroyColorSetting.get()
            ? this.colorSheydera2Setting.get()
            : this.colorSheydera1Setting.get();
      }
   }

   public float calculateValue38() {
      return this.dlinaPriEdeSetting.get();
   }

   public int calculateValue5() {
      return this.autoColorPodTemuSetting.get()
         ? ClickGuiRenderer.getThemeColor(ThemeColorRole.MAIN)
         : this.colorZalivkiSetting.get();
   }

   public boolean checkCondition20() {
      return this.sohranitTeniSetting.get();
   }

   public boolean checkCondition12() {
      return this.lentyPriUdareSetting.get();
   }

   public boolean checkCondition2() {
      return this.modeSetting.is("Заливка");
   }

   public boolean checkCondition19() {
      return this.modeSetting.is("Nothing");
   }

   public boolean checkCondition16() {
      return this.obvodkaSetting.get();
   }

   public void updateState2() {
      if (this.stateActive2) {
         stateActive = false;
         renderTarget = null;
         this.stateActive2 = false;
         if (isEnabled() && this.textureTarget != null && Minecraft.getInstance().gameRenderer.mainRenderTarget() != null) {
            HandsCompositeRenderer.handsCompositeRenderer
               .updateState2(this.textureTarget, Minecraft.getInstance().gameRenderer.mainRenderTarget(), this);
         }
      }
   }

   public boolean checkCondition3() {
      return this.modeSetting.is("Шейдер") || this.checkCondition6();
   }

   public boolean checkCondition() {
      return this.sohranitTeniSheyderaSetting.get();
   }

   public float calculateValue32() {
      return this.razmytieSetting.get();
   }

   public boolean checkCondition10() {
      return this.checkCondition7() && this.modeShleyfaSetting.is("Энергия");
   }

   public float calculateValue46() {
      return this.brightnessEnergiiSetting.get();
   }

   private Hands() {
      this.modeSetting = new ModeSettings(
         "Режим",
         "Nothing",
         "Заливка",
         "Шейдер",
         "Зеркало",
         "Шейдер + Зеркало"
      );
      this.autoColorPodTemuSetting = new BooleanSetting("Авто цвет под тему", true);
      this.autoColorPodPredmetSetting = new BooleanSetting("Авто цвет под предмет", false);
      this.smeshivanieSetting = new SliderSetting("Смешивание", 0.0F, 1.0F, 0.01F, 0.5F)
         .visibleWhen(this::checkCondition4);
      this.iskazhenieZerkalaSetting = new SliderSetting("Искажение зеркала", 0.0F, 1.0F, 0.05F, 0.0F)
         .visibleWhen(this::checkCondition4);
      this.strengthRazmytiyaSetting = new SliderSetting("Сила размытия", 0.1F, 3.0F, 0.05F, 1.0F)
         .visibleWhen(this::checkCondition4);
      this.raduzhnayaSetting = new BooleanSetting("Радужная", false).visibleWhen(this::checkCondition2);
      this.speedRadugiSetting = new SliderSetting("Скорость радуги", 0.0F, 2.0F, 0.05F, 0.4F)
         .visibleWhen(() -> this.checkCondition2() && this.raduzhnayaSetting.get());
      this.masshtabRadugiSetting = new SliderSetting("Масштаб радуги", 0.2F, 3.0F, 0.1F, 1.0F)
         .visibleWhen(() -> this.checkCondition2() && this.raduzhnayaSetting.get());
      this.colorZalivkiSetting = new ColorSetting("Цвет заливки", -48060, false)
         .visibleWhen(
            () -> this.checkCondition2()
               && !this.raduzhnayaSetting.get()
               && !this.autoColorPodTemuSetting.get()
         );
      this.opacityZalivkiSetting = new SliderSetting("Прозрачность заливки", 0.0F, 1.0F, 0.05F, 0.8F)
         .visibleWhen(this::checkCondition2);
      this.sohranitTeniSetting = new BooleanSetting("Сохранить тени", true).visibleWhen(this::checkCondition2);
      this.strengthTeneySetting = new SliderSetting("Сила теней", 0.0F, 1.0F, 0.05F, 0.3F)
         .visibleWhen(() -> this.checkCondition2() && this.sohranitTeniSetting.get());
      this.stilSheyderaSetting = new ModeSettings(
            "Стиль шейдера",
            "Шейдер",
            "HandShader",
            "Туманность",
            "Космос",
            "Градиент",
            "Homie"
         )
         .visibleWhen(this::checkCondition3);
      this.colorSheydera1Setting = new ColorSetting("Цвет шейдера 1", -3632385, false)
         .visibleWhen(() -> this.checkCondition3() && !this.autoColorPodTemuSetting.get());
      this.vtoroyColorSetting = new BooleanSetting("Второй цвет", false).visibleWhen(this::checkCondition3);
      this.colorSheydera2Setting = new ColorSetting("Цвет шейдера 2", -11534136, false)
         .visibleWhen(
            () -> this.checkCondition3()
               && this.vtoroyColorSetting.get()
               && !this.autoColorPodTemuSetting.get()
         );
      this.speedSheyderaSetting = new SliderSetting("Скорость шейдера", 0.05F, 2.5F, 0.05F, 1.5F)
         .visibleWhen(this::checkCondition3);
      this.masshtabSheyderaSetting = new SliderSetting("Масштаб шейдера", 0.2F, 5.0F, 0.1F, 1.5F)
         .visibleWhen(this::checkCondition3);
      this.svechenieSheyderaSetting = new SliderSetting("Свечение шейдера", 0.0F, 3.0F, 0.05F, 1.2F)
         .visibleWhen(this::checkCondition3);
      this.kachestvoSheyderaSetting = new SliderSetting("Качество шейдера", 1.0F, 3.0F, 1.0F, 2.0F)
         .visibleWhen(() -> this.checkCondition3() && !this.stilSheyderaSetting.is("Градиент"));
      this.zvyozdySetting = new BooleanSetting("Звёзды", true).visibleWhen(this::checkCondition3);
      this.opacitySheyderaSetting = new SliderSetting("Прозрачность шейдера", 0.0F, 1.0F, 0.05F, 1.0F)
         .visibleWhen(this::checkCondition3);
      this.sohranitTeniSheyderaSetting = new BooleanSetting("Сохранить тени шейдера", false).visibleWhen(this::checkCondition3);
      this.strengthTeneySheyderaSetting = new SliderSetting("Сила теней шейдера", 0.0F, 1.0F, 0.05F, 0.5F)
         .visibleWhen(() -> this.checkCondition3() && this.sohranitTeniSheyderaSetting.get());
      this.glovSetting = new BooleanSetting("Глов", true);
      this.razmytieSetting = new SliderSetting("Размытие", 1.0F, 5.0F, 1.0F, 3.0F)
         .visibleWhen(this.glovSetting::get);
      this.brightnessSetting = new SliderSetting("Яркость", 0.5F, 5.0F, 0.1F, 1.0F)
         .visibleWhen(this.glovSetting::get);
      this.colorGlova1Setting = new ColorSetting("Цвет глова 1", -11713, false)
         .visibleWhen(
            () -> this.glovSetting.get()
               && !this.autoColorPodTemuSetting.get()
               && !this.autoColorPodPredmetSetting.get()
         );
      this.colorGlova2Setting = new ColorSetting("Цвет глова 2", -54784, false)
         .visibleWhen(
            () -> this.glovSetting.get()
               && !this.autoColorPodTemuSetting.get()
               && !this.autoColorPodPredmetSetting.get()
         );
      this.vnutrenneeSvechenieSetting = new SliderSetting("Внутреннее свечение", 0.0F, 1.5F, 0.05F, 0.65F)
         .visibleWhen(this.glovSetting::get);
      this.obvodkaSetting = new BooleanSetting("Обводка", true);
      this.colorObvodkiSetting = new ColorSetting("Цвет обводки", -1, true)
         .visibleWhen(
            () -> this.obvodkaSetting.get()
               && !this.autoColorPodTemuSetting.get()
               && !this.autoColorPodPredmetSetting.get()
         );
      this.tolshchinaObvodkiSetting = new SliderSetting("Толщина обводки", 0.5F, 2.0F, 0.5F, 1.0F)
         .visibleWhen(this.obvodkaSetting::get);
      this.shleyfSetting = new BooleanSetting("Шлейф", true)
         .visibleWhen(this.glovSetting::get);
      this.modeShleyfaSetting = new ModeSettings(
            "Режим шлейфа",
            "Обычный",
            "Энергия",
            "Ленты"
         )
         .visibleWhen(this::checkCondition7);
      this.dlinaEnergiiSetting = new SliderSetting("Длина энергии", 0.4F, 2.0F, 0.05F, 1.0F)
         .visibleWhen(this::checkCondition10);
      this.brightnessEnergiiSetting = new SliderSetting("Яркость энергии", 0.5F, 2.5F, 0.05F, 1.35F)
         .visibleWhen(this::checkCondition10);
      this.movementEnergiiSetting = new SliderSetting("Движение энергии", 0.0F, 2.0F, 0.05F, 1.3F)
         .visibleWhen(this::checkCondition10);
      this.pulsatsiyaEnergiiSetting = new SliderSetting("Пульсация энергии", 0.0F, 2.0F, 0.05F, 1.0F)
         .visibleWhen(this::checkCondition10);
      this.shleyfZaKameroySetting = new SliderSetting("Шлейф за камерой", 0.0F, 2.0F, 0.05F, 0.05F)
         .visibleWhen(this::checkCondition10);
      this.dlinaLentSetting = new SliderSetting("Длина лент", 0.15F, 3.0F, 0.05F, 0.3F)
         .visibleWhen(this::checkCondition14);
      this.widthLentSetting = new SliderSetting("Ширина лент", 0.25F, 1.5F, 0.05F, 0.55F)
         .visibleWhen(this::checkCondition14);
      this.brightnessLentSetting = new SliderSetting("Яркость лент", 0.0F, 2.5F, 0.05F, 1.0F)
         .visibleWhen(this::checkCondition14);
      this.strengthLentZaKameroySetting = new SliderSetting("Сила лент за камерой", 0.0F, 2.0F, 0.05F, 0.1F)
         .visibleWhen(this::checkCondition14);
      this.podyomLentSetting = new SliderSetting("Подъём лент", 0.0F, 1.0F, 0.01F, 0.32F)
         .visibleWhen(this::checkCondition14);
      this.izgibLentSetting = new SliderSetting("Изгиб лент", 0.0F, 2.0F, 0.05F, 2.0F)
         .visibleWhen(this::checkCondition14);
      this.speedVolnSetting = new SliderSetting("Скорость волн", 0.0F, 3.0F, 0.05F, 1.5F)
         .visibleWhen(this::checkCondition14);
      this.lentyPriUdareSetting = new BooleanSetting("Ленты при ударе", true).visibleWhen(this::checkCondition14);
      this.dlinaPriUdareSetting = new SliderSetting("Длина при ударе", 0.15F, 3.0F, 0.05F, 0.15F)
         .visibleWhen(() -> this.checkCondition14() && this.lentyPriUdareSetting.get());
      this.strengthPriUdareSetting = new SliderSetting("Сила при ударе", 0.0F, 2.5F, 0.05F, 1.15F)
         .visibleWhen(() -> this.checkCondition14() && this.lentyPriUdareSetting.get());
      this.lentyPriEdeSetting = new BooleanSetting("Ленты при еде", true).visibleWhen(this::checkCondition14);
      this.dlinaPriEdeSetting = new SliderSetting("Длина при еде", 0.15F, 3.0F, 0.05F, 0.65F)
         .visibleWhen(() -> this.checkCondition14() && this.lentyPriEdeSetting.get());
      this.strengthPriEdeSetting = new SliderSetting("Сила при еде", 0.0F, 2.5F, 0.05F, 0.7F)
         .visibleWhen(() -> this.checkCondition14() && this.lentyPriEdeSetting.get());
      this.speedZatuhaniyaSetting = new SliderSetting("Скорость затухания", 0.002F, 0.2F, 0.002F, 0.04F)
         .visibleWhen(this::checkCondition11);
      this.podyomSetting = new SliderSetting("Подъём", 0.0F, 1.5F, 0.05F, 0.2F)
         .visibleWhen(this::checkCondition11);
      this.kachanieSetting = new SliderSetting("Качание", 0.0F, 0.2F, 0.005F, 0.0F)
         .visibleWhen(this::checkCondition11);
      this.shleyfOtKamerySetting = new SliderSetting("Шлейф от камеры", 0.0F, 2.0F, 0.05F, 0.0F)
         .visibleWhen(this::checkCondition11);
      this.turbulentnostSetting = new SliderSetting("Турбулентность", 0.0F, 0.6F, 0.01F, 0.02F)
         .visibleWhen(this::checkCondition11);
      this.mertsanieSetting = new SliderSetting("Мерцание", 0.0F, 0.2F, 0.01F, 0.2F)
         .visibleWhen(this::checkCondition11);
      this.sduvPriUdareSetting = new BooleanSetting("Сдув при ударе", false).visibleWhen(this::checkCondition11);
      this.strengthSduvaSetting = new SliderSetting("Сила сдува", 1.0F, 10.0F, 0.5F, 4.0F)
         .visibleWhen(() -> this.checkCondition11() && this.sduvPriUdareSetting.get());
      this.settings = 
         new Setting[]{
            this.modeSetting,
            this.autoColorPodTemuSetting,
            this.autoColorPodPredmetSetting,
            this.smeshivanieSetting,
            this.iskazhenieZerkalaSetting,
            this.strengthRazmytiyaSetting,
            this.raduzhnayaSetting,
            this.speedRadugiSetting,
            this.masshtabRadugiSetting,
            this.colorZalivkiSetting,
            this.opacityZalivkiSetting,
            this.sohranitTeniSetting,
            this.strengthTeneySetting,
            this.stilSheyderaSetting,
            this.colorSheydera1Setting,
            this.vtoroyColorSetting,
            this.colorSheydera2Setting,
            this.speedSheyderaSetting,
            this.masshtabSheyderaSetting,
            this.svechenieSheyderaSetting,
            this.kachestvoSheyderaSetting,
            this.zvyozdySetting,
            this.opacitySheyderaSetting,
            this.sohranitTeniSheyderaSetting,
            this.strengthTeneySheyderaSetting,
            this.glovSetting,
            this.razmytieSetting,
            this.brightnessSetting,
            this.colorGlova1Setting,
            this.colorGlova2Setting,
            this.vnutrenneeSvechenieSetting,
            this.obvodkaSetting,
            this.colorObvodkiSetting,
            this.tolshchinaObvodkiSetting,
            this.shleyfSetting,
            this.modeShleyfaSetting,
            this.dlinaEnergiiSetting,
            this.brightnessEnergiiSetting,
            this.movementEnergiiSetting,
            this.pulsatsiyaEnergiiSetting,
            this.shleyfZaKameroySetting,
            this.dlinaLentSetting,
            this.widthLentSetting,
            this.brightnessLentSetting,
            this.strengthLentZaKameroySetting,
            this.podyomLentSetting,
            this.izgibLentSetting,
            this.speedVolnSetting,
            this.lentyPriUdareSetting,
            this.dlinaPriUdareSetting,
            this.strengthPriUdareSetting,
            this.lentyPriEdeSetting,
            this.dlinaPriEdeSetting,
            this.strengthPriEdeSetting,
            this.speedZatuhaniyaSetting,
            this.podyomSetting,
            this.kachanieSetting,
            this.shleyfOtKamerySetting,
            this.turbulentnostSetting,
            this.mertsanieSetting,
            this.sduvPriUdareSetting,
            this.strengthSduvaSetting
         }
      ;
   }

   public float calculateValue8() {
      return this.masshtabSheyderaSetting.get();
   }

   public float calculateValue35() {
      return this.strengthPriEdeSetting.get();
   }

   public void onEnable() {
      this.lastUpdateMillis = System.currentTimeMillis();
   }

   public float calculateValue24() {
      return this.kachanieSetting.get();
   }

   public float calculateValue28() {
      return this.opacityZalivkiSetting.get();
   }

   public int calculateValue29() {
      return ClickGuiRenderer.getThemeColor(ThemeColorRole.MAIN);
   }

   public float calculateValue11() {
      return this.izgibLentSetting.get();
   }

   public boolean checkCondition17() {
      return this.sduvPriUdareSetting.get();
   }

   public boolean checkCondition8() {
      return this.lentyPriEdeSetting.get();
   }

   public float calculateValue41() {
      return this.shleyfZaKameroySetting.get();
   }

   public float calculateValue25() {
      return this.shleyfOtKamerySetting.get();
   }

   public float calculateValue3() {
      return this.podyomSetting.get();
   }

   public float calculateValue4() {
      return this.strengthPriUdareSetting.get();
   }

   public boolean checkCondition4() {
      return this.modeSetting.is("Зеркало") || this.checkCondition6();
   }

   public float calculateValue36() {
      return this.speedSheyderaSetting.get();
   }

   public boolean checkCondition14() {
      return this.checkCondition7() && this.modeShleyfaSetting.is("Ленты");
   }

   public int calculateValue21() {
      if (this.autoColorPodPredmetSetting.get()) {
         return -1;
      } else {
         return this.autoColorPodTemuSetting.get()
            ? ClickGuiRenderer.getThemeColor(ThemeColorRole.MAIN)
            : this.colorObvodkiSetting.get();
      }
   }

   public float calculateValue18() {
      return this.masshtabRadugiSetting.get();
   }

   public float calculateValue7() {
      return this.iskazhenieZerkalaSetting.get();
   }

   public int calculateValue42() {
      if (this.stilSheyderaSetting.is("HandShader")) {
         return 4;
      } else if (this.stilSheyderaSetting.is("Туманность")) {
         return 1;
      } else if (this.stilSheyderaSetting.is("Космос")) {
         return 2;
      } else if (this.stilSheyderaSetting.is("Градиент")) {
         return 3;
      } else {
         return this.stilSheyderaSetting.is("Homie") ? 5 : 0;
      }
   }

   public float calculateValue49() {
      return this.tolshchinaObvodkiSetting.get();
   }

   public float calculateValue13() {
      return this.speedZatuhaniyaSetting.get();
   }

   public float calculateValue14() {
      return this.podyomLentSetting.get();
   }

   public float calculateValue27() {
      return this.brightnessLentSetting.get();
   }

   public float calculateValue() {
      return this.speedVolnSetting.get();
   }

   public boolean checkCondition11() {
      return this.checkCondition7() && this.modeShleyfaSetting.is("Обычный");
   }

   public int calculateValue51() {
      return this.autoColorPodTemuSetting.get()
         ? ClickGuiRenderer.getThemeColor(ThemeColorRole.MAIN)
         : this.colorGlova1Setting.get();
   }

   public float calculateValue45() {
      return this.dlinaEnergiiSetting.get();
   }

   public float calculateValue43() {
      return this.strengthRazmytiyaSetting.get();
   }

   public float calculateValue20() {
      return this.mertsanieSetting.get();
   }

   public float calculateValue19() {
      return this.widthLentSetting.get();
   }

   public int calculateValue22() {
      return this.autoColorPodTemuSetting.get()
         ? ClickGuiRenderer.getThemeColor(ThemeColorRole.MAIN_TWO)
         : this.colorGlova2Setting.get();
   }

   public float calculateValue6() {
      return this.strengthTeneySetting.get();
   }

   public boolean checkCondition6() {
      return this.modeSetting.is("Шейдер + Зеркало");
   }

   public float calculateValue34() {
      return this.strengthLentZaKameroySetting.get();
   }

   public float calculateValue9() {
      return this.strengthSduvaSetting.get();
   }

   public void onDisable() {
      stateActive = false;
      renderTarget = null;
      this.stateActive2 = false;
      if (this.textureTarget != null) {
         this.textureTarget.destroyBuffers();
         this.textureTarget = null;
      }

      HandsCompositeRenderer.handsCompositeRenderer.updateState();
   }

   public float calculateValue37() {
      return this.turbulentnostSetting.get();
   }

   public boolean checkCondition13() {
      return isEnabled()
         && (!this.checkCondition19() || this.glovSetting.get() || this.obvodkaSetting.get());
   }

   public float calculateValue48() {
      return this.vnutrenneeSvechenieSetting.get();
   }

   public float calculateValue12() {
      return (float)(System.currentTimeMillis() - this.lastUpdateMillis) / 1000.0F;
   }

   public boolean checkCondition7() {
      return this.glovSetting.get() && this.shleyfSetting.get();
   }

   public float calculateValue47() {
      return this.opacitySheyderaSetting.get();
   }

   public boolean checkCondition18() {
      return this.autoColorPodPredmetSetting.get();
   }

   public boolean checkCondition5() {
      return this.zvyozdySetting.get();
   }

   public boolean checkCondition15() {
      return this.raduzhnayaSetting.get();
   }

   public int calculateValue39() {
      return this.checkCondition14() ? 2 : (this.checkCondition10() ? 1 : 0);
   }

   public float calculateValue50() {
      return this.strengthTeneySheyderaSetting.get();
   }

   public boolean checkCondition9() {
      return this.glovSetting.get();
   }

   public float calculateValue30() {
      return this.movementEnergiiSetting.get();
   }

   public static boolean isEnabled() {
      return ClientModules.HANDS != null && ClientModules.HANDS.enabled();
   }

   /** Called every client tick; frees the offscreen targets once the module is switched off. */
   public static void tick() {
      boolean enabled = isEnabled();
      if (enabled != INSTANCE.wasEnabled) {
         INSTANCE.wasEnabled = enabled;
         if (enabled) INSTANCE.onEnable(); else INSTANCE.onDisable();
      }
   }
}