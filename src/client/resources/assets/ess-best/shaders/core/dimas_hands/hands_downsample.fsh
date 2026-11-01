#version 330

uniform sampler2D Sampler0;

in vec2 TexCoord;
out vec4 fragColor;

void main() {
    vec2 offset = 0.5 / vec2(textureSize(Sampler0, 0));
    fragColor = (texture(Sampler0, TexCoord - offset)
            + texture(Sampler0, TexCoord + offset)
            + texture(Sampler0, TexCoord + vec2(offset.x, -offset.y))
            + texture(Sampler0, TexCoord + vec2(-offset.x, offset.y))) * 0.25;
}
