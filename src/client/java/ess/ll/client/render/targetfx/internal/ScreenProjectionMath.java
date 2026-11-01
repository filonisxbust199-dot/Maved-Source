package ess.ll.client.render.targetfx.internal;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import net.minecraft.world.phys.Vec3;

/** Matrices captured from the live world render for source GPU effects. */
public final class ScreenProjectionMath {
    private static final Matrix4f projection=new Matrix4f(),view=new Matrix4f(),cameraEffects=new Matrix4f();
    private static Vec3 cameraPosition=Vec3.ZERO;
    private static float partialTick;
    private ScreenProjectionMath() {}
    public static void captureViewProjection(Matrix4fc v,Matrix4fc p) {
        view.set(v);projection.set(p);
    }
    public static void captureViewProjection(Matrix4fc v,Matrix4fc p,Vec3 position,float partial) {
        view.set(v);projection.set(p);cameraPosition=position;partialTick=partial;
    }
    public static void captureCameraEffects(Matrix4fc effects) { cameraEffects.set(effects); }
    public static Matrix4f cameraEffectsCopy() { return new Matrix4f(cameraEffects); }
    public static Vec3 cameraPosition(){return cameraPosition;}
    public static float partialTick(){return partialTick;}
    public static Matrix4f projectionMatrixCopy() { return new Matrix4f(projection); }
    public static Matrix4f viewMatrixCopy() { return new Matrix4f(view); }
}
