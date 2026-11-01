#include<rounded-base>

#inputs {
    vec2 offset;
    vec2 halfSize;
    float radius;
};

uniform sampler2D u_Texture;
uniform vec2 inverseScreenSize;

void main() {
    float distance = roundedBoxSDF(offset, halfSize, radius);

    vec3 background = inverseScreenSize.x > 0.0
        ? texture(u_Texture, gl_FragCoord.xy * inverseScreenSize).rgb
        : texelFetch(u_Texture, ivec2(gl_FragCoord.xy), 0).rgb;
    fragColor = vec4(background, 1. - smoothstep(0., edgeSoftness, distance)) * shaderColor;
}
