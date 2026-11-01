#include<rounded-base>

#inputs {
    vec4 vertexColor;
    vec2 offset;
    vec2 halfSize;
    float radius;
    float param;
};

void main() {
    float sd = roundedBoxSDF(offset, halfSize, radius);

    // param = softness (px). The squared smooth curve has zero slope at both ends, avoiding
    // a dark contour at the panel edge while keeping most of the density close to the panel.
    float t = clamp(sd / max(param, 0.0001), 0., 1.);
    float smoothFade = 1. - t * t * (3. - 2. * t);
    float falloff = smoothFade * smoothFade;

    fragColor = vec4(vertexColor.rgb, falloff * vertexColor.a) * shaderColor;
}
