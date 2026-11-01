#version 150
#moj_import <ess-best:wet_pro.glsl>
in vec2 vUv;
out vec4 fragColor;

void main() {
    fragColor = vec4(0);
    if (WetReflectionDistanceCoverage.y <= .001) return;
    vec3 p, n, world; float pool;
    float wet = surfaceAt(vUv, p, n, world, pool);
    if (wet <= .003 || pool < .01) return;
    vec3 reflected = normalize(reflect(normalize(p), waterNormal(n, world.xz, pool)));
    vec3 origin = p + n * max(.025, length(p) * .0007);
    float stepSize = max(.15, length(p) * .012);
    float travel = stepSize * (.5 + hash(floor(vUv * Resolution.zw)) * .5);
    float previous = 0.0;
    for (int i = 0; i < 40; ++i) {
        if (i >= int(SkySteps.w) || travel > WetReflectionDistanceCoverage.z || (origin + reflected * travel).z > -.02) break;
        vec3 ray = origin + reflected * travel;
        bool valid; vec2 uv = project(ray, valid);
        if (!valid) break;
        float depth = texture(DepthSampler, uv).r;
        if (depth > 0.000001) {
            vec3 seen = positionFromDepth(uv, depth);
            float delta = seen.z - ray.z;
            if (i > 1 && delta > 0.0 && delta < max(.12, stepSize * 1.7) && dot(seen - p, n) > .04) {
                float lo = previous, hi = travel;
                for (int refine = 0; refine < 5; ++refine) {
                    float mid = (lo + hi) * .5;
                    vec3 q = origin + reflected * mid;
                    bool inside; vec2 candidate = project(q, inside);
                    if (!inside) break;
                    float d = texture(DepthSampler, candidate).r;
                    if (d > 0.000001 && positionFromDepth(candidate, d).z > q.z) hi = mid;
                    else lo = mid;
                }
                vec2 hitUv = project(origin + reflected * hi, valid);
                vec3 hitPosition = positionFromDepth(hitUv, texture(DepthSampler, hitUv).r);
                if (valid && dot(hitPosition - p, n) > .03) {
                    float edge = min(min(hitUv.x, hitUv.y), min(1.0 - hitUv.x, 1.0 - hitUv.y));
                    float confidence = smoothstep(0.0, .075, edge) * (1.0 - smoothstep(WetReflectionDistanceCoverage.z * .6, WetReflectionDistanceCoverage.z, hi));
                    fragColor = vec4(texture(Scene, hitUv).rgb, confidence);
                    return;
                }
            }
        }
        previous = travel;
        travel += stepSize;
        stepSize *= 1.14;
    }
}
