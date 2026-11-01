package ess.ll.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ess.ll.client.module.render.CustomCrystal;
import ess.ll.client.module.render.CustomCrystalEffects;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.crystal.EndCrystalModel;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep ESS's separate rotation, bobbing, and base controls at the model animation site. */
@Mixin(EndCrystalModel.class)
public abstract class CustomCrystalModelMixin {
    @Shadow @Final public ModelPart outerGlass;
    @Shadow @Final public ModelPart innerGlass;
    @Shadow @Final public ModelPart cube;

    @ModifyExpressionValue(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;)V",
            at=@At(value="FIELD", target="Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;showsBottom:Z"))
    private boolean ess$crystalBase(boolean original) {
        return original && (CustomCrystalEffects.capturing() || CustomCrystal.base());
    }

    @ModifyExpressionValue(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;)V",
            at=@At(value="FIELD", target="Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;ageInTicks:F", ordinal=0))
    private float ess$crystalSpin(float original) {
        return original * (CustomCrystalEffects.capturing() ? 1f : CustomCrystal.spin());
    }

    @ModifyExpressionValue(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;)V",
            at=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/entity/EndCrystalRenderer;getY(F)F"))
    private float ess$crystalBob(float original) {
        if(CustomCrystalEffects.capturing()) return original;
        // getY includes a constant offset; only scale the oscillation around its resting height.
        return -1f + (original + 1f) * CustomCrystal.bob();
    }

    @Inject(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;)V",at=@At("TAIL"))
    private void ess$astralMotion(EndCrystalRenderState state,CallbackInfo ci) {
        outerGlass.skipDraw=false;
        innerGlass.skipDraw=false;
        cube.visible=true;
        if(CustomCrystalEffects.capturing() || !CustomCrystal.astral()) return;
        float time=state.ageInTicks*.05f*CustomCrystal.ASTRAL_SPEED.get();
        // Render state has no entity ID in 26.3; block coordinates keep the phase stable across interpolated frames.
        int seed=(int)Math.floor(state.x)*92821+(int)Math.floor(state.y)*40503+(int)Math.floor(state.z)*6151;
        float phase=time+(seed&1023)*.31f;
        switch (CustomCrystal.ASTRAL_SHAPE.get()) {
            case "Вихрь" -> {
                outerGlass.yScale*=1.25f;
                outerGlass.rotateBy(new Quaternionf().rotationY(phase*.65f).rotateZ(.22f));
                innerGlass.rotateBy(new Quaternionf().rotationY(-phase*1.3f));
                innerGlass.yScale*=.8f;
            }
            case "Созвездие" -> {
                innerGlass.rotateBy(new Quaternionf().rotationY(-phase*.4f).rotateX(.3f));
                cube.visible=false;
            }
            case "Контур" -> { cube.visible=false; outerGlass.y=24; }
            case "Грани" -> {
                outerGlass.yScale*=1.15f;
                innerGlass.rotateBy(new Quaternionf().rotationY(phase*.7f));
                cube.rotateBy(new Quaternionf().rotationZ(-phase*.45f));
            }
            case "Ядро" -> {
                outerGlass.skipDraw=true;
                innerGlass.skipDraw=true;
                cube.xScale*=.95f; cube.yScale*=.95f; cube.zScale*=.95f;
                cube.rotateBy(new Quaternionf().rotationY(-phase*.6f));
            }
        }
        float scale=1,lift=0,yaw=0,tilt=0,innerYaw=0,innerTilt=0;
        switch (CustomCrystal.ASTRAL_ANIMATION.get()) {
            case "Пульсация" -> scale=1+(float)Math.sin(phase*2.8f)*.14f;
            case "Маятник" -> { lift=(float)Math.sin(phase*1.1f)*.12f; yaw=(float)Math.sin(phase*1.8f)*.4f; }
            case "Резонанс" -> {
                scale=1+(float)Math.sin(phase*4)*.05f;
                yaw=(float)Math.sin(phase*1.5f)*.3f;
                innerYaw=-phase*.8f;
                innerTilt=(float)Math.sin(phase*2.1f)*.25f;
            }
            case "Гироскоп" -> { yaw=phase*.45f; tilt=phase*.7f; innerYaw=-phase*.8f; innerTilt=phase*.55f; }
            case "Парение" -> lift=(float)Math.sin(phase)*.18f;
            case "Кувырок" -> { yaw=phase*1.1f; tilt=phase*.9f; innerYaw=phase*.5f; innerTilt=-phase*.6f; }
        }
        outerGlass.y+=lift*8;
        outerGlass.rotateBy(new Quaternionf().rotationY(yaw).rotateZ(tilt));
        innerGlass.rotateBy(new Quaternionf().rotationY(innerYaw).rotateX(innerTilt));
        outerGlass.xScale*=scale; outerGlass.yScale*=scale; outerGlass.zScale*=scale;
    }
}
