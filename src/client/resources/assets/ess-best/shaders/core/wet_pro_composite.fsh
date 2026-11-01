#version 150
#moj_import <ess-best:wet_pro.glsl>
uniform sampler2D Reflection;
in vec2 vUv;
out vec4 fragColor;

vec4 reconstructReflection(vec3 position, vec3 normal, float roughness) {
    vec2 grid = vUv * Resolution.zw - .5;
    vec2 base = floor(grid), f = fract(grid);
    vec4 sum = vec4(0); float weightSum = 0.0;
    // Native texel centers plus bilateral rejection: no blurred reflection leaking
    // across a foreground silhouette into the floor behind it.
    for (int j = 0; j < 2; ++j) for (int i = 0; i < 2; ++i) {
        vec2 uv = safeUv((base + vec2(i, j) + .5) / Resolution.zw);
        float depth = texture(DepthSampler, uv).r;
        if (depth <= 0.000001) continue;
        vec3 q = positionFromDepth(uv, depth);
        float planeError = abs(dot(q - position, normal));
        float weight = (i == 0 ? 1.0 - f.x : f.x) * (j == 0 ? 1.0 - f.y : f.y);
        weight *= exp(-planeError / max(.03, length(position) * .003));
        vec4 reflection = texture(Reflection, uv);
        sum += vec4(reflection.rgb * reflection.a, reflection.a) * weight;
        weightSum += weight;
    }
    if (weightSum < .0001 || sum.a < .00001) return vec4(0);
    vec4 result = vec4(sum.rgb / sum.a, sum.a / weightSum);
    if (roughness > .05) {
        vec2 offset = vec2(0, roughness * 3.0 / Resolution.w);
        vec4 a = texture(Reflection, safeUv(vUv + offset)), b = texture(Reflection, safeUv(vUv - offset));
        // Only soften within the same surface plane.
        vec3 qa = positionFromDepth(safeUv(vUv + offset), texture(DepthSampler, safeUv(vUv + offset)).r);
        vec3 qb = positionFromDepth(safeUv(vUv - offset), texture(DepthSampler, safeUv(vUv - offset)).r);
        float wa = a.a * exp(-abs(dot(qa - position, normal)) / max(.03, length(position) * .003));
        float wb = b.a * exp(-abs(dot(qb - position, normal)) / max(.03, length(position) * .003));
        result.rgb = (result.rgb + a.rgb * wa * roughness + b.rgb * wb * roughness) / (1.0 + (wa + wb) * roughness);
    }
    return result;
}

void main() {
    vec4 original = texture(Scene, vUv);
    fragColor = original;
    vec3 p, n, world; float pool;
    float wet = surfaceAt(vUv, p, n, world, pool);
    if (wet <= .002) return;
    vec3 water = waterNormal(n, world.xz, pool);
    vec3 direction = normalize(reflect(normalize(p), water));
    vec3 worldDirection = normalize(mat3(InvView) * direction);
    vec3 environment = mix(SkySteps.rgb * .35, SkySteps.rgb * .86 + vec3(.1, .12, .15), sat(worldDirection.y * .7 + .3));
    // Keep the fallback in world directions. Switching to a screen sky pixel
    // at an occluder or frame edge created a sudden change when pitching.
    vec4 reflection = reconstructReflection(p, n, SizeSoftnessFilmRoughness.w);
    vec3 reflected = mix(environment, reflection.rgb, reflection.a);
    reflected *= mix(vec3(1), Tint.rgb * 1.35, Tint.a);
    float noV = sat(dot(water, normalize(-p)));
    float fresnel = .035 + .965 * pow(1.0 - noV, 5.0);
    float mirror = wet * pool * WetReflectionDistanceCoverage.y * (0.20 + fresnel * 1.6);
    mirror *= 1.0 - SizeSoftnessFilmRoughness.w * .45;
    vec3 soaked = original.rgb * (1.0 - wet * (.14 + .17 * pool));
    soaked = mix(soaked, soaked * Tint.rgb * 1.3, Tint.a * .35);
    vec3 color = mix(soaked, reflected, min(.92, mirror));
    color += reflected * fresnel * wet * .035 * WetReflectionDistanceCoverage.y;
    fragColor = vec4(clamp(color, 0.0, 1.0), original.a);
}
