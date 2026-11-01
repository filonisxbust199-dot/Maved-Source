#version 330

#moj_import <ess-best:dimas_hands_item_color.glsl>

uniform sampler2D BloomSampler;
uniform sampler2D HandMaskSampler;

layout(std140) uniform HandsGlow {
    vec4 Params;
    vec4 GlowColor1;
    vec4 GlowColor2;
    vec4 OutlineColor;
    vec4 Effects;
};

in vec2 TexCoord;
out vec4 fragColor;

float coverageAt(vec2 uv) {
    return texture(HandMaskSampler, uv).r;
}

void main() {
    vec2 texel = 1.0 / max(Effects.xy, vec2(1.0));
    vec2 low = texel * 0.5;
    vec2 high = vec2(1.0) - low;
    vec2 safeUv = clamp(TexCoord, low, high);
    vec4 bloom = texture(BloomSampler, safeUv);
    float bloomMask = handsBloomMask(bloom, Params.y);
    if (bloomMask < 0.002) discard;
    float coverage = coverageAt(safeUv);
    if (coverage >= 0.22) discard;

    if (coverage < 0.5 && bloomMask > 0.08) {
        float left = coverageAt(safeUv - vec2(texel.x, 0.0));
        float right = coverageAt(safeUv + vec2(texel.x, 0.0));
        float down = coverageAt(safeUv - vec2(0.0, texel.y));
        float up = coverageAt(safeUv + vec2(0.0, texel.y));
        coverage = max(coverage, max(min(left, right), min(down, up)));
        if (coverage < 0.5 && left + right + down + up > 1.5) coverage = 1.0;
    }
    float handMask = smoothstep(0.01, 0.22, coverage);
    float bloomAlpha = bloomMask * (1.0 - handMask);
    if (bloomAlpha < 0.002) discard;

    float intensity = bloomAlpha * Params.x;
    float core = pow(clamp(bloomAlpha, 0.0, 1.0), 0.55);
    vec3 baseColor = mix(GlowColor2.rgb, GlowColor1.rgb, core);
    if (Params.y > 0.5) baseColor = handsBloomColor(bloom);
    fragColor = vec4(max(baseColor, vec3(0.0)), clamp(intensity, 0.0, 1.0));
}
