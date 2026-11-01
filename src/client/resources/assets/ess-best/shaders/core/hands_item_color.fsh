#version 150
#moj_import <ess-best:hands_item_color.glsl>
uniform sampler2D HandsSampler;
in vec2 texCoord;
out vec4 fragColor;
vec4 itemSample(vec2 uv) {
    if (any(lessThan(uv, vec2(0.0))) || any(greaterThan(uv, vec2(1.0)))) return vec4(0.0);
    vec4 hand = texture(HandsSampler, uv);
    return vec4(handsItemColor(hand) * hand.a, hand.a);
}
void main() {
    vec2 offset = 0.5 / vec2(textureSize(HandsSampler, 0));
    fragColor = (itemSample(texCoord - offset) + itemSample(texCoord + offset)
            + itemSample(texCoord + vec2(offset.x, -offset.y))
            + itemSample(texCoord + vec2(-offset.x, offset.y))) * 0.25;
}
