#version 150

// Outline kawase upsample
uniform sampler2D Sampler0;

layout(std140) uniform KawaseData {
    vec4 Params; // x=width, y=height, z=offset, w=0
};

in vec2 texCoord;
out vec4 fragColor;

vec4 tap(vec2 p) {
    if (p.x < 0.0 || p.x > 1.0 || p.y < 0.0 || p.y > 1.0) return vec4(0.0);
    return texture(Sampler0, p);
}

void main() {
    vec2 halfPixel = vec2(Params.z / max(Params.x, 1.0), Params.z / max(Params.y, 1.0));
    vec4 sum = tap(texCoord + vec2(-halfPixel.x * 2.0, 0.0));
    sum += tap(texCoord + vec2(-halfPixel.x, halfPixel.y)) * 2.0;
    sum += tap(texCoord + vec2(0.0, halfPixel.y * 2.0));
    sum += tap(texCoord + vec2(halfPixel.x, halfPixel.y)) * 2.0;
    sum += tap(texCoord + vec2(halfPixel.x * 2.0, 0.0));
    sum += tap(texCoord + vec2(halfPixel.x, -halfPixel.y)) * 2.0;
    sum += tap(texCoord + vec2(0.0, -halfPixel.y * 2.0));
    sum += tap(texCoord + vec2(-halfPixel.x, -halfPixel.y)) * 2.0;
    fragColor = sum / 12.0;
}
