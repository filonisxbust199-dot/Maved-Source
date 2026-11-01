#version 330

uniform sampler2D Sampler0;

in vec2 TexCoord;
out vec4 fragColor;

void main() {
#ifdef HANDS_MASK
    fragColor = vec4(texture(Sampler0, TexCoord).r);
#else
    fragColor = texture(Sampler0, TexCoord);
#endif
}
