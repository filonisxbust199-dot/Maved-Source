#version 150

// Dual-filter Kawase downsample — halfPixel = offset / size
uniform sampler2D Sampler0;

layout(std140) uniform KawaseData {
    vec4 Params; // x=width, y=height, z=offset, w=0
};

in vec2 texCoord;
out vec4 fragColor;

vec4 tap(vec2 p) {
    p = clamp(p, vec2(0.001), vec2(0.999));
    return texture(Sampler0, p);
}

void main() {
    vec2 halfPixel = vec2(Params.z / max(Params.x, 1.0), Params.z / max(Params.y, 1.0));
    vec4 sum = tap(texCoord) * 4.0;
    sum += tap(texCoord - halfPixel);
    sum += tap(texCoord + halfPixel);
    sum += tap(texCoord + vec2(halfPixel.x, -halfPixel.y));
    sum += tap(texCoord - vec2(halfPixel.x, -halfPixel.y));
    fragColor = sum / 8.0;
}
