#version 150

uniform sampler2D Sampler0;

layout(std140) uniform BlurParams {
    vec4 Blur; // xy = per-tap uv step (direction * spread / size)
};

in vec2 texCoord;
out vec4 fragColor;

// separable 9-tap gaussian; run once horizontally, once vertically
void main() {
    float w0 = 0.227027;
    float w1 = 0.194595;
    float w2 = 0.121622;
    float w3 = 0.054054;
    float w4 = 0.016216;

    vec2 s = Blur.xy;
    vec4 c = texture(Sampler0, texCoord) * w0;
    c += texture(Sampler0, texCoord + s * 1.0) * w1;
    c += texture(Sampler0, texCoord - s * 1.0) * w1;
    c += texture(Sampler0, texCoord + s * 2.0) * w2;
    c += texture(Sampler0, texCoord - s * 2.0) * w2;
    c += texture(Sampler0, texCoord + s * 3.0) * w3;
    c += texture(Sampler0, texCoord - s * 3.0) * w3;
    c += texture(Sampler0, texCoord + s * 4.0) * w4;
    c += texture(Sampler0, texCoord - s * 4.0) * w4;
    fragColor = c;
}
