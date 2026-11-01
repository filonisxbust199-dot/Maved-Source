#version 330

#moj_import <ess-best:dimas_hands_item_color.glsl>

uniform sampler2D BloomSampler;
uniform sampler2D HandMaskSampler;
uniform sampler2D HandSampler;

layout(std140) uniform HandsGlow {
    vec4 Params;
    vec4 GlowColor1;
    vec4 GlowColor2;
    vec4 OutlineColor;
    vec4 Effects;
};

in vec2 TexCoord;
out vec4 fragColor;

void main() {
    vec2 texel = 1.0 / max(Effects.xy, vec2(1.0));
    vec2 uv = clamp(TexCoord, texel * 0.5, vec2(1.0) - texel * 0.5);
    vec4 bloomSample = texture(BloomSampler, uv);
    float bloom = handsBloomMask(bloomSample, Params.y);
    if (bloom < 0.0005) discard;
    float center = texture(HandMaskSampler, uv).r;
    if (center >= 0.22) discard;

    vec4 sides = vec4(
            texture(HandMaskSampler, uv - vec2(texel.x, 0.0)).r,
            texture(HandMaskSampler, uv + vec2(texel.x, 0.0)).r,
            texture(HandMaskSampler, uv - vec2(0.0, texel.y)).r,
            texture(HandMaskSampler, uv + vec2(0.0, texel.y)).r);
    vec4 corners = vec4(
            texture(HandMaskSampler, uv - texel).r,
            texture(HandMaskSampler, uv + texel).r,
            texture(HandMaskSampler, uv + vec2(-texel.x, texel.y)).r,
            texture(HandMaskSampler, uv + vec2(texel.x, -texel.y)).r);
    float sideBridge = max(min(sides.x, sides.y), min(sides.z, sides.w));
    float cornerBridge = max(min(corners.x, corners.y), min(corners.z, corners.w));
    float sideCount = dot(sides, vec4(1.0));
    float closed = max(center, max(max(sideBridge, cornerBridge), step(5.5, sideCount + dot(corners, vec4(1.0)))));
    float outlineMask = smoothstep(0.01, 0.22, closed);
    float outlineAlpha = 0.0;
    if (outlineMask < 0.999) {
        float neighbor;
        if (Effects.z == 1.0) {
            vec4 nearby = max(sides, corners);
            neighbor = max(max(nearby.x, nearby.y), max(nearby.z, nearby.w));
        } else {
            vec2 offset = texel * Effects.z;
            vec4 edgeSides = vec4(
                    texture(HandMaskSampler, uv - vec2(offset.x, 0.0)).r,
                    texture(HandMaskSampler, uv + vec2(offset.x, 0.0)).r,
                    texture(HandMaskSampler, uv - vec2(0.0, offset.y)).r,
                    texture(HandMaskSampler, uv + vec2(0.0, offset.y)).r);
            vec2 diagonal = offset * 0.7071;
            vec4 edgeCorners = vec4(
                    texture(HandMaskSampler, uv - diagonal).r,
                    texture(HandMaskSampler, uv + diagonal).r,
                    texture(HandMaskSampler, uv + vec2(-diagonal.x, diagonal.y)).r,
                    texture(HandMaskSampler, uv + vec2(diagonal.x, -diagonal.y)).r);
            vec4 nearby = max(edgeSides, edgeCorners);
            neighbor = max(max(nearby.x, nearby.y), max(nearby.z, nearby.w));
        }
        outlineAlpha = smoothstep(0.02, 0.28, neighbor) * (1.0 - outlineMask) * OutlineColor.a;
        if (outlineAlpha < 0.002) outlineAlpha = 0.0;
    }
    vec3 outlineColor = OutlineColor.rgb;
    if (Params.y > 0.5 && outlineAlpha > 0.0) {
        outlineColor = handsOutlineColor(HandSampler, uv, texel, Effects.z, handsBloomColor(bloomSample));
    }
    if (outlineAlpha >= 1.0) {
        fragColor = vec4(outlineColor, 1.0);
        return;
    }

    float coverage = center;
    if (bloom > 0.08) {
        coverage = max(coverage, sideBridge);
        if (coverage < 0.5 && sideCount > 1.5) coverage = 1.0;
    }
    float glowAlpha = bloom * (1.0 - smoothstep(0.01, 0.22, coverage));
    vec3 glow = vec3(0.0);
    if (glowAlpha >= 0.002) {
        float core = pow(clamp(glowAlpha, 0.0, 1.0), 0.55);
        vec3 color = Params.y > 0.5 ? handsBloomColor(bloomSample) : mix(GlowColor2.rgb, GlowColor1.rgb, core);
        glow = color * clamp(glowAlpha * Params.x, 0.0, 1.0);
    } else if (outlineAlpha == 0.0) {
        discard;
    }
    fragColor = vec4(mix(glow, outlineColor, outlineAlpha), outlineAlpha);
}
