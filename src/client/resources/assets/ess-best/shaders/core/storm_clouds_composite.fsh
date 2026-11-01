#version 150
#moj_import <ess-best:storm_clouds.glsl>
uniform sampler2D Clouds;
in vec2 vUv;
out vec4 fragColor;
void main() {
    vec2 grid = vUv * Resolution.zw - .5;
    vec2 base = floor(grid), f = fract(grid);
    float distance = sceneDistance(vUv);
    vec4 sum = vec4(0); float total = 0.0;
    for (int j = 0; j < 2; ++j) for (int i = 0; i < 2; ++i) {
        vec2 uv = safeUv((base + vec2(i, j) + .5) / Resolution.zw);
        float sampledDistance = sceneDistance(uv);
        float weight = (i == 0 ? 1.0 - f.x : f.x) * (j == 0 ? 1.0 - f.y : f.y);
        weight *= exp(-abs(sampledDistance - distance) / max(1.0, distance * .03));
        sum += texture(Clouds, uv) * weight;
        total += weight;
    }
    fragColor = total > .0001 ? sum / total : vec4(0);
}
