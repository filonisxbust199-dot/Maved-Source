#version 330

uniform sampler2D Sampler0;

in vec2 TexCoord;
out vec4 fragColor;

void main() {
    vec2 offset = 0.75 / vec2(textureSize(Sampler0, 0));
    float coverage = max(max(texture(Sampler0, TexCoord - offset).r,
                             texture(Sampler0, TexCoord + offset).r),
                         max(texture(Sampler0, TexCoord + vec2(offset.x, -offset.y)).r,
                             texture(Sampler0, TexCoord + vec2(-offset.x, offset.y)).r));
    fragColor = vec4(coverage);
}
