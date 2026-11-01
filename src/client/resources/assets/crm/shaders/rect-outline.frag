#include<rounded-base>

#inputs {
    vec4 vertexColor;
    vec2 offset;
    vec2 halfSize;
    float radius;
    float param;
};

// rounded.glsl's roundedBoxSDF clamps to 0 inside the rect — fine for edge AA, useless for a
// stroke band that lives at negative distances. This is the full signed version (interior term).
float sdRoundBoxFull(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}

void main() {
    float sd = sdRoundBoxFull(offset, halfSize, radius);

    // param = thickness (px). Inset stroke: the band covers sd in [-param, 0], so the outline
    // hugs the rect edge without growing its footprint; edgeSoftness AA on both rims.
    float outer = 1. - smoothstep(0., edgeSoftness, sd);
    float inner = smoothstep(-param - edgeSoftness, -param, sd);

    fragColor = vec4(vertexColor.rgb, outer * inner * vertexColor.a) * shaderColor;
}
