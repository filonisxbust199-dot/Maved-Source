#version 150
uniform sampler2D VisibleSampler;
uniform sampler2D GlowSampler;
uniform sampler2D WideGlowSampler;
uniform sampler2D BodyDepthSampler;
uniform sampler2D SceneDepthSampler;
layout(std140) uniform ChamsComposite { vec4 Settings; };
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec4 body = texture(VisibleSampler, texCoord);
    vec3 closeGlow = texture(GlowSampler, texCoord).rgb;
    vec3 wideGlow = texture(WideGlowSampler, texCoord).rgb;
    // The ordinary visible surface is already filled in 3D. Repaint only fragments
    // hidden by world/equipment when the optional x-ray view requests them.
    float occluded = step(texture(BodyDepthSampler, texCoord).r + 0.000001,
            texture(SceneDepthSampler, texCoord).r);
    float alpha = body.a * Settings.x * Settings.z * occluded;
    // A tight luminous edge plus a broad falloff, rather than a single faint blur.
    // A small amount also lights the filled surface, giving the model a neon core.
    vec3 light = (closeGlow * 1.25 + wideGlow * 1.35) * Settings.y;
    // Compress brightness with one shared RGB factor so the rim stays the chosen
    // neon hue instead of clipping individual channels into a white outline.
    float peak = max(light.r, max(light.g, light.b));
    vec3 halo = light / (1.0 + peak);
    vec3 emission = mix(halo, light * 0.06, body.a) * Settings.x;
    fragColor = vec4(body.rgb * alpha + emission, alpha);
}
