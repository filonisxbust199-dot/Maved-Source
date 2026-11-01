#version 330

uniform sampler2D TrailSampler;

in vec2 TexCoord;
out vec4 fragColor;

void main() {
    fragColor = texture(TrailSampler, TexCoord);
}
