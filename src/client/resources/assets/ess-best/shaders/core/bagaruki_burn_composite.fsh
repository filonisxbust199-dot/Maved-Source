#version 150

// Nightix burn_composite — additive glow + trail onto the main framebuffer.
uniform sampler2D MaskSampler;
uniform sampler2D GlowSampler;
uniform sampler2D TrailSampler;

layout(std140) uniform BurnData {
    vec4 ResTime;   // width, height, time, fillMode
    vec4 Flame;     // radiusPx, strength, flameSpeed, colorMix
    vec4 Trail;     // decay, flameHeightPx, flowSpeed, trailStrength
    vec4 FireColor; // rgb, glowStrength
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    float mask = texture(MaskSampler, texCoord).a;
    vec4 glow = texture(GlowSampler, texCoord);
    vec4 trail = texture(TrailSampler, texCoord);
    float fill = ResTime.w;
    // Flame strength is already applied when building glow; do not square it again.
    float strength = max(FireColor.w, 0.05);

    // keep most of the fire outside the solid hand; burn-through lifts the gate
    float gate = mix(1.0 - mask * 0.88, 1.0, fill);

    // premultiplied-style add
    vec3 add = (glow.rgb + trail.rgb * 1.15) * gate * strength;
    float a = clamp((glow.a * 1.05 + trail.a * 0.95) * gate * strength, 0.0, 1.0);

    if (dot(add, add) < 1e-6 && a < 0.003) discard;

    // additive blend expects non-premult or ONE,ONE with rgb; we output lit rgb + alpha
    fragColor = vec4(add, a);
}
