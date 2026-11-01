package ess.ll.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.io.IOException;

/** Loads the original Hex particle sprites for the particle atlas. */
public enum ClientTextures {
    INSTANCE;

    public Identifier squareTexture() { return Identifier.fromNamespaceAndPath("hex", "pictures/kvadrat.png"); }
    public Identifier orbTexture() { return Identifier.fromNamespaceAndPath("hex", "particles/orbiznew.png"); }

    private static final String[] PARTICLES = {
        "orbiznew", "star", "heart", "dollar", "snowflake", "crown",
        "lightning", "line", "point", "rhombus", "spark"
    };

    public NativeImage loadParticleImage(int index) {
        if (index < 0 || index >= PARTICLES.length) return null;
        Identifier id = Identifier.fromNamespaceAndPath("hex", "particles/" + PARTICLES[index] + ".png");
        var resource = Minecraft.getInstance().getResourceManager().getResource(id);
        if (resource.isEmpty()) return null;
        try (var input = resource.get().open()) {
            return NativeImage.read(input);
        } catch (IOException exception) {
            return null;
        }
    }
}
