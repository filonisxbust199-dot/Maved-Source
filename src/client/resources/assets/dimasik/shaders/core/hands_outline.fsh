#version 330
#extension GL_ARB_separate_shader_objects : require

#include <dimasik:hands_item_color.glsl>

uniform sampler2D HandMaskSampler;
uniform sampler2D BloomSampler;
uniform sampler2D HandSampler;

layout(std140) uniform HandsGlow {
    vec4 Params;
    vec4 GlowColor1;
    vec4 GlowColor2;
    vec4 OutlineColor;
    vec4 Effects;
};

layout(location = 0) in vec2 TexCoord;
layout(location = 0) out vec4 fragColor;

float coverageAt(vec2 uv) {
    return texture(HandMaskSampler, uv).r;
}

void main() {
    vec2 texel = 1.0 / max(Effects.xy, vec2(1.0));
    vec2 safeUv = clamp(TexCoord, texel * 0.5, vec2(1.0) - texel * 0.5);
    vec4 bloom = texture(BloomSampler, safeUv);
    if (handsBloomMask(bloom, Params.y) < 0.0005) discard;
    float handAlpha = coverageAt(safeUv);
    if (handAlpha >= 0.22) discard;

    float left = coverageAt(safeUv - vec2(texel.x, 0.0));
    float right = coverageAt(safeUv + vec2(texel.x, 0.0));
    float down = coverageAt(safeUv - vec2(0.0, texel.y));
    float up = coverageAt(safeUv + vec2(0.0, texel.y));
    float downLeft = coverageAt(safeUv - texel);
    float upRight = coverageAt(safeUv + texel);
    float upLeft = coverageAt(safeUv + vec2(-texel.x, texel.y));
    float downRight = coverageAt(safeUv + vec2(texel.x, -texel.y));
    float bridge = max(max(min(left, right), min(down, up)),
            max(min(downLeft, upRight), min(upLeft, downRight)));
    float neighborCount = left + right + down + up + downLeft + upRight + upLeft + downRight;
    float closedAlpha = max(handAlpha, max(bridge, step(5.5, neighborCount)));
    float handMask = smoothstep(0.01, 0.22, closedAlpha);
    if (handMask >= 0.999) discard;

    float neighborMask;
    if (Effects.z == 1.0) {
        neighborMask = max(max(max(left, right), max(down, up)),
                max(max(downLeft, upRight), max(upLeft, downRight)));
    } else {
        vec2 outlineStep = texel * max(Effects.z, 0.0);
        neighborMask = 0.0;
        neighborMask = max(neighborMask, coverageAt(safeUv + vec2(outlineStep.x, 0.0)));
        neighborMask = max(neighborMask, coverageAt(safeUv - vec2(outlineStep.x, 0.0)));
        neighborMask = max(neighborMask, coverageAt(safeUv + vec2(0.0, outlineStep.y)));
        neighborMask = max(neighborMask, coverageAt(safeUv - vec2(0.0, outlineStep.y)));
        neighborMask = max(neighborMask, coverageAt(safeUv + outlineStep * 0.7071));
        neighborMask = max(neighborMask, coverageAt(safeUv - outlineStep * 0.7071));
        neighborMask = max(neighborMask, coverageAt(safeUv + vec2(outlineStep.x, -outlineStep.y) * 0.7071));
        neighborMask = max(neighborMask, coverageAt(safeUv + vec2(-outlineStep.x, outlineStep.y) * 0.7071));
    }

    float outline = smoothstep(0.02, 0.28, neighborMask) * (1.0 - handMask);
    float alpha = outline * OutlineColor.a;
    if (alpha < 0.002) discard;
    vec3 color = Params.y > 0.5
            ? handsOutlineColor(HandSampler, safeUv, texel, Effects.z, handsBloomColor(bloom))
            : OutlineColor.rgb;
    fragColor = vec4(color, alpha);
}
