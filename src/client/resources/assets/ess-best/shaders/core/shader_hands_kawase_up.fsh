#version 150

// Kawase dual-filter upsample (порт hands_kawase_up.fsh, color=white).
uniform sampler2D InSampler;

layout(std140) uniform KawaseSettings {
    vec4 KawaseParams; // xy: halfPixel * offset
};

in vec2 texCoord;
out vec4 fragColor;

vec4 tap(vec2 p) {
    if (p.x < 0.0 || p.x > 1.0 || p.y < 0.0 || p.y > 1.0) return vec4(0.0);
    return texture(InSampler, p);
}

void main() {
    vec2 h = KawaseParams.xy;
    vec4 sum = tap(texCoord + vec2(-h.x * 2.0, 0.0));
    sum += tap(texCoord + vec2(-h.x, h.y)) * 2.0;
    sum += tap(texCoord + vec2(0.0, h.y * 2.0));
    sum += tap(texCoord + vec2(h.x, h.y)) * 2.0;
    sum += tap(texCoord + vec2(h.x * 2.0, 0.0));
    sum += tap(texCoord + vec2(h.x, -h.y)) * 2.0;
    sum += tap(texCoord + vec2(0.0, -h.y * 2.0));
    sum += tap(texCoord + vec2(-h.x, -h.y)) * 2.0;
    fragColor = sum / 12.0;
}
