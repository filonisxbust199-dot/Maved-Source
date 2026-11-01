#version auto

precision highp float;

#include<rounded, shaderColor>

#uniforms {
    float Time;
};

#inputs {
    vec4 vertexColor;
    vec2 offset;
    vec2 halfSize;
    float radius;
    float param;
};

out vec4 fragColor;

// highp on purpose: Time reaches ~1000s inside cos() phases (same reasoning as shimmer-rect)
float edgeSoftness; #constant<default = 2.0>

// full signed SDF — the stroke band lives at negative distances, the library one is 0 inside
float sdRoundBoxFull(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}

// Rotating gradient frame: the outline band (param = thickness, inset like rect-outline) with
// two opposite highlight arcs sweeping around it — the angular mask is just cos(angle - Time),
// so arcs of every rect in the batch spin in phase from one uniform.
void main() {
    float sd = sdRoundBoxFull(offset, halfSize, radius);
    float outer = 1. - smoothstep(0., edgeSoftness, sd);
    float inner = smoothstep(-param - edgeSoftness, -param, sd);
    float band = outer * inner;

    float ang = atan(offset.y, offset.x);
    float head = pow(0.5 + 0.5 * cos(ang - Time * 1.9), 5.0);
    float tail = pow(0.5 + 0.5 * cos(ang - Time * 1.9 + 3.14159265), 5.0);

    vec3 c = clamp(vertexColor.rgb * (0.75 + 0.7 * (head + tail * 0.5)), 0., 1.);
    float a = band * clamp(0.22 + head + tail * 0.55, 0., 1.) * vertexColor.a;

    fragColor = vec4(c, a) * shaderColor;
}
