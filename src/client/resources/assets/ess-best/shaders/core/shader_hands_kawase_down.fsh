#version 150

// Kawase dual-filter downsample (порт hands_kawase_down.fsh). Тапы за границей экрана
// возвращают прозрачный 0 (у старой системы был CLAMP_TO_BORDER; clamp-to-edge размазывал
// бы свечение от краёв экрана бесконечной полосой).
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
    vec4 sum = tap(texCoord) * 4.0;
    sum += tap(texCoord - h);
    sum += tap(texCoord + h);
    sum += tap(texCoord + vec2(h.x, -h.y));
    sum += tap(texCoord - vec2(h.x, -h.y));
    fragColor = sum / 8.0;
}
