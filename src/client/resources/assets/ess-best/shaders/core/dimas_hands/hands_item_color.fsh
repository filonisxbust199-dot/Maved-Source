#version 330

#moj_import <ess-best:dimas_hands_item_color.glsl>

uniform sampler2D HandSampler;
uniform sampler2D HandMaskSampler;

in vec2 TexCoord;
out vec4 fragColor;

vec4 itemSample(vec2 uv) {
    float coverage = texture(HandMaskSampler, uv).r;
    if (coverage <= 0.0) return vec4(0.0);
    return vec4(handsItemColor(texture(HandSampler, uv)) * coverage, coverage);
}

void main() {
    vec2 offset = 0.5 / vec2(textureSize(HandSampler, 0));
    fragColor = (itemSample(TexCoord - offset)
            + itemSample(TexCoord + offset)
            + itemSample(TexCoord + vec2(offset.x, -offset.y))
            + itemSample(TexCoord + vec2(-offset.x, offset.y))) * 0.25;
}
