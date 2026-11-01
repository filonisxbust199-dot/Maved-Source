package ess.ll.client.module.render;
import ess.ll.client.module.ClientModules;

/** Smooth zoom: the camera FOV is divided by an eased factor that follows the Zoom module state. */
public final class ZoomModule {
    private static float factor=1;
    private static long lastNanos=System.nanoTime();
    private ZoomModule() {}
    /** Called once per FOV calculation (every frame); returns the FOV with the current zoom applied. */
    public static float apply(float fov) {
        long now=System.nanoTime();
        float dt=(float)Math.min(.1,(now-lastNanos)/1e9);
        lastNanos=now;
        float target=ClientModules.ZOOM.enabled() ? ClientModules.ZOOM_FACTOR.get() : 1;
        // Exponential approach in log space so zooming in and out feel equally fast.
        float logFactor=(float)Math.log(factor), logTarget=(float)Math.log(target);
        logFactor+=(logTarget-logFactor)*(1-(float)Math.exp(-dt*ClientModules.ZOOM_SPEED.get()));
        factor=Math.abs(logTarget-logFactor)<.001f ? target : (float)Math.exp(logFactor);
        return fov/factor;
    }
    /** Current zoom factor, 1 when not zoomed; used to scale mouse sensitivity. */
    public static float factor() { return factor; }
}
