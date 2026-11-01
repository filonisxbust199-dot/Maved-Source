#include<rounded-base>

#inputs {
    vec2 texPos;
    vec4 vertexColor;
    vec2 offset;
    vec2 halfSize;
    float radius;
};

uniform sampler2D u_Texture;

void main() {
    float distance = roundedBoxSDF(offset, halfSize, radius);

    vec4 col = vertexColor * texture(u_Texture, texPos);

    fragColor = vec4(col.rgb, (1. - smoothstep(0., edgeSoftness, distance)) * col.a) * shaderColor;
}