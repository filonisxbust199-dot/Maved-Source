#version 150

// Kawase dual-filter downsample (Nightix ShaderEsp / ESS hands port)
uniform sampler2D Sampler0;

layout(std140) uniform EspKawaseData {
    vec4 KawaseParams; // xy = halfPixel, z = offset
};

in vec2 texCoord;
out vec4 fragColor;

vec4 tap(vec2 p) {
    if (p.x < 0.0 || p.x > 1.0 || p.y < 0.0 || p.y > 1.0) return vec4(0.0);
    return texture(Sampler0, p);
}

void main() {
    vec2 h = KawaseParams.xy * max(KawaseParams.z, 0.5);
    vec4 sum = tap(texCoord) * 4.0;
    sum += tap(texCoord - h);
    sum += tap(texCoord + h);
    sum += tap(texCoord + vec2(h.x, -h.y));
    sum += tap(texCoord - vec2(h.x, -h.y));
    fragColor = sum / 8.0;
}
