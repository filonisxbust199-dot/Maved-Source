#version auto

precision highp float;

#include<rounded, shaderColor>

#uniforms {
    float Time;
};

#inputs {
    vec4 vertexColor;
    vec4 vertexColor2;
    vec2 offset;
    vec2 halfSize;
    float radius;
    vec2 flow;
};

out vec4 fragColor;

// highp on purpose: Time reaches ~1000s and rides into sin() phases — rounded-base's lowp
// default would make the animation chunky on strict-precision drivers. AA matches its 2px.
float edgeSoftness; #constant<default = 2.0>

void main() {
    float sd = roundedBoxSDF(offset, halfSize, radius);
    float mask = 1. - smoothstep(0., edgeSoftness, sd);

    // flow encodes both direction (its normal) and speed (its length): (1,0)*s flows
    // horizontally, (0,1)*s vertically, any diagonal in between — one batch, mixed rects
    float speed = length(flow);
    vec2 dir = flow / max(speed, 0.0001);
    vec2 uv = offset / max(halfSize, vec2(1.)) * 0.5;   // -0.5..0.5 across the rect
    float t = dot(uv, dir);

    // colors slide along the axis: one full A->B->A cycle across the rect, phase runs on Time
    float wave = 0.5 + 0.5 * sin(t * 6.2831853 - Time * speed);
    vec3 color = mix(vertexColor.rgb, vertexColor2.rgb, wave);

    // narrow glare stripe chasing ahead of the wave — the "перелив" highlight
    float glare = pow(0.5 + 0.5 * sin(t * 6.2831853 - Time * speed * 1.45 + 1.3), 10.0);
    color += vec3(glare * 0.12);

    float alpha = mix(vertexColor.a, vertexColor2.a, wave);
    fragColor = vec4(color, alpha * mask) * shaderColor;
}
