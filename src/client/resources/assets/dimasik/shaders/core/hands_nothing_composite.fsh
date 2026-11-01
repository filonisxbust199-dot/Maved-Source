#version 330
#extension GL_ARB_separate_shader_objects : require

#include <dimasik:hands_item_color.glsl>

uniform sampler2D HandSampler;
uniform sampler2D HandMaskSampler;
uniform sampler2D BloomSampler;

layout(std140) uniform HandsData {
    vec4 Params0;
    vec4 Params1;
    vec4 Params2;
    vec4 Params3;
    vec4 Params4;
    vec4 Params5;
    vec4 Params6;
    vec4 ShaderColor1;
    vec4 ShaderColor2;
    vec4 FillColor;
    vec4 MirrorColor;
    vec4 GlowColor1;
    vec4 GlowColor2;
    vec2 Size;
};

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

vec4 repairedHandSample(vec2 uv) {
    vec4 center = texture(HandSampler, uv);
    if (center.a >= 0.999) return center;

    if (center.a > 0.001) {
        vec3 restoredColor = clamp(center.rgb / max(center.a, 0.03125), 0.0, 1.0);
        return vec4(restoredColor, 1.0);
    }

    vec2 texel = 1.0 / max(Size, vec2(1.0));
    vec2 low = texel * 0.5;
    vec2 high = vec2(1.0) - low;
    vec2 safeUv = clamp(uv, low, high);

    float support = handsBloomMask(texture(BloomSampler, safeUv), Params5.y);
    if (support < 0.006) return center;

    float centerCoverage = texture(HandMaskSampler, safeUv).r;
    vec2 stepX = vec2(texel.x, 0.0);
    vec2 stepY = vec2(0.0, texel.y);

    if (centerCoverage >= 0.5) return vec4(center.rgb, 1.0);

    float leftCoverage = texture(HandMaskSampler, clamp(safeUv - stepX, low, high)).r;
    float rightCoverage = texture(HandMaskSampler, clamp(safeUv + stepX, low, high)).r;
    float downCoverage = texture(HandMaskSampler, clamp(safeUv - stepY, low, high)).r;
    float upCoverage = texture(HandMaskSampler, clamp(safeUv + stepY, low, high)).r;
    float bridge = max(min(leftCoverage, rightCoverage), min(downCoverage, upCoverage));

    float diagonalBridge = 0.0;
    if (centerCoverage < 0.5 && bridge < 0.5 && support > 0.12) {
        float downLeftCoverage = texture(HandMaskSampler, clamp(safeUv - texel, low, high)).r;
        float upRightCoverage = texture(HandMaskSampler, clamp(safeUv + texel, low, high)).r;
        float upLeftCoverage = texture(HandMaskSampler,
                clamp(safeUv + vec2(-texel.x, texel.y), low, high)).r;
        float downRightCoverage = texture(HandMaskSampler,
                clamp(safeUv + vec2(texel.x, -texel.y), low, high)).r;
        diagonalBridge = max(
                min(downLeftCoverage, upRightCoverage),
                min(upLeftCoverage, downRightCoverage)
        );
    }

    float wideBridge = 0.0;
    if (centerCoverage < 0.5 && max(bridge, diagonalBridge) < 0.5 && support > 0.32) {
        vec2 wideX = stepX * 2.0;
        vec2 wideY = stepY * 2.0;
        float farLeft = texture(HandMaskSampler, clamp(safeUv - wideX, low, high)).r;
        float farRight = texture(HandMaskSampler, clamp(safeUv + wideX, low, high)).r;
        float farDown = texture(HandMaskSampler, clamp(safeUv - wideY, low, high)).r;
        float farUp = texture(HandMaskSampler, clamp(safeUv + wideY, low, high)).r;
        wideBridge = max(min(farLeft, farRight), min(farDown, farUp));
    }

    if (max(centerCoverage, max(bridge, max(diagonalBridge, wideBridge))) < 0.5) return center;

    vec4 left = texture(HandSampler, clamp(safeUv - stepX, low, high));
    vec4 right = texture(HandSampler, clamp(safeUv + stepX, low, high));
    vec4 down = texture(HandSampler, clamp(safeUv - stepY, low, high));
    vec4 up = texture(HandSampler, clamp(safeUv + stepY, low, high));
    float weight = left.a + right.a + down.a + up.a;
    vec3 weightedColor = left.rgb * left.a + right.rgb * right.a
            + down.rgb * down.a + up.rgb * up.a;

    if (weight <= 0.001) {
        vec4 downLeft = texture(HandSampler, clamp(safeUv - texel, low, high));
        vec4 upRight = texture(HandSampler, clamp(safeUv + texel, low, high));
        vec4 upLeft = texture(HandSampler,
                clamp(safeUv + vec2(-texel.x, texel.y), low, high));
        vec4 downRight = texture(HandSampler,
                clamp(safeUv + vec2(texel.x, -texel.y), low, high));
        weight = downLeft.a + upRight.a + upLeft.a + downRight.a;
        weightedColor = downLeft.rgb * downLeft.a + upRight.rgb * upRight.a
                + upLeft.rgb * upLeft.a + downRight.rgb * downRight.a;
    }
    if (weight <= 0.001 && wideBridge >= 0.5) {
        vec2 wideX = stepX * 2.0;
        vec2 wideY = stepY * 2.0;
        vec4 farLeft = texture(HandSampler, clamp(safeUv - wideX, low, high));
        vec4 farRight = texture(HandSampler, clamp(safeUv + wideX, low, high));
        vec4 farDown = texture(HandSampler, clamp(safeUv - wideY, low, high));
        vec4 farUp = texture(HandSampler, clamp(safeUv + wideY, low, high));
        weight = farLeft.a + farRight.a + farDown.a + farUp.a;
        weightedColor = farLeft.rgb * farLeft.a + farRight.rgb * farRight.a
                + farDown.rgb * farDown.a + farUp.rgb * farUp.a;
    }
    if (weight <= 0.001) return center;
    return vec4(weightedColor / weight, 1.0);
}

void main() {
    vec4 hand = repairedHandSample(texCoord);
    if (hand.a <= 0.001) discard;

    vec3 result = hand.rgb;
    if (Params4.z > 0.5 && Params5.w > 0.001 && hand.a > 0.08) {
        float bloomMask = clamp(handsBloomMask(texture(BloomSampler, texCoord), Params5.y), 0.0, 1.0);
        if (bloomMask > 0.008 && bloomMask < 0.92) {
            float edgeDistance = 1.0 - smoothstep(0.18, 0.92, bloomMask);
            float edgeSupport = smoothstep(0.008, 0.22, bloomMask);
            float innerGlow = hand.a * pow(edgeDistance, 0.72) * edgeSupport * Params5.w;
            if (innerGlow > 0.08) {
                float stableHandMask = smoothstep(0.08, 0.32, hand.a);
                float innerCore = smoothstep(0.08, 0.72, innerGlow) * stableHandMask;
                float core = pow(clamp(innerGlow, 0.0, 1.0), 0.55);
                vec3 innerColor = mix(GlowColor2.rgb, GlowColor1.rgb, core);
                if (Params5.y > 0.5) innerColor = handsItemColor(hand);
                result = mix(result, innerColor, innerCore * 0.42);
                result *= 1.0 + innerCore * 0.14;
            }
        }
    }

    fragColor = vec4(max(result, vec3(0.0)), smoothstep(0.01, 0.16, hand.a));
}
