#version 150
#moj_import <ess-best:depth.glsl>
in vec2 vUv;
out vec4 fragColor;
uniform sampler2D u_SceneTexture;
uniform sampler2D u_DepthTexture;
layout(std140) uniform WetParams {
    mat4 u_ViewMatrix;
    mat4 u_ProjectionMatrix;
    mat4 u_InverseProjectionMatrix;
    mat4 u_InverseViewMatrix;
    vec4 ResolutionTimeWetness;
    vec4 PuddleReflectionDistanceRipple;
    vec4 RainStepsRipples;
    vec4 CameraOrigin;
    vec4 SkyColor;
};
#define u_Resolution ResolutionTimeWetness.xy
#define u_Time ResolutionTimeWetness.z
#define u_Wetness ResolutionTimeWetness.w
#define u_PuddleCoverage PuddleReflectionDistanceRipple.x
#define u_ReflectionStrength PuddleReflectionDistanceRipple.y
#define u_MaxDistance PuddleReflectionDistanceRipple.z
#define u_RippleStrength PuddleReflectionDistanceRipple.w
#define u_RainAmount RainStepsRipples.x
#define u_ReflectionSteps int(RainStepsRipples.y)
#define u_Ripples int(RainStepsRipples.z)

const int MAX_REFLECTION_STEPS = 16;

float saturate(float value) {
    return clamp(value, 0.0, 1.0);
}

vec2 safeUv(vec2 uv) {
    return clamp(uv, vec2(0.001), vec2(0.999));
}

float luminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float puddleNoise(vec2 p) {
    float value = valueNoise(p) * 0.56;
    value += valueNoise(p * 2.03 + vec2(17.7, 4.3)) * 0.29;
    value += valueNoise(p * 4.11 + vec2(-8.1, 13.2)) * 0.15;
    return value;
}

vec3 reconstructViewPosition(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, essDepthToNdc(depth), 1.0);
    vec4 view = u_InverseProjectionMatrix * clip;
    float invW = abs(view.w) > 0.000001 ? 1.0 / view.w : 1.0;
    return view.xyz * invW;
}

vec3 reconstructWorldPosition(vec3 viewPosition) {
    return (u_InverseViewMatrix * vec4(viewPosition, 1.0)).xyz + CameraOrigin.xyz;
}

vec3 reconstructViewNormal(vec2 uv, vec3 centerPosition) {
    vec2 texel = 1.0 / max(u_Resolution, vec2(1.0));

    float depthL = texture(u_DepthTexture, safeUv(uv - vec2(texel.x, 0.0))).r;
    float depthR = texture(u_DepthTexture, safeUv(uv + vec2(texel.x, 0.0))).r;
    float depthD = texture(u_DepthTexture, safeUv(uv - vec2(0.0, texel.y))).r;
    float depthU = texture(u_DepthTexture, safeUv(uv + vec2(0.0, texel.y))).r;

    vec3 posL = reconstructViewPosition(uv - vec2(texel.x, 0.0), max(depthL, 0.000001));
    vec3 posR = reconstructViewPosition(uv + vec2(texel.x, 0.0), max(depthR, 0.000001));
    vec3 posD = reconstructViewPosition(uv - vec2(0.0, texel.y), max(depthD, 0.000001));
    vec3 posU = reconstructViewPosition(uv + vec2(0.0, texel.y), max(depthU, 0.000001));

    vec3 rightA = posR - centerPosition;
    vec3 rightB = centerPosition - posL;
    vec3 upA = posU - centerPosition;
    vec3 upB = centerPosition - posD;
    vec3 tangentX = dot(rightA, rightA) < dot(rightB, rightB) ? rightA : rightB;
    vec3 tangentY = dot(upA, upA) < dot(upB, upB) ? upA : upB;

    vec3 normal = cross(tangentX, tangentY);
    float normalLength = length(normal);
    if (normalLength <= 0.000001) {
        return vec3(0.0, 0.0, 1.0);
    }
    normal /= normalLength;
    if (dot(normal, -centerPosition) < 0.0) normal = -normal;
    return normal;
}

float cellRipple(vec2 worldXZ, float scale, float speed, vec2 offset) {
    vec2 grid = worldXZ * scale + offset;
    vec2 cell = floor(grid);
    vec2 local = fract(grid) - 0.5;
    float seed = hash21(cell + offset * 3.7);
    vec2 center = vec2(
        hash21(cell + vec2(3.1, 7.9)),
        hash21(cell + vec2(11.7, -5.3))
    );
    center = (center - 0.5) * 0.54;

    float phase = fract(u_Time * speed + seed);
    float radius = phase * 0.72;
    float distanceToDrop = length(local - center);
    float ringDistance = distanceToDrop - radius;
    float envelope = exp(-abs(ringDistance) * 27.0) * (1.0 - phase);
    return sin(ringDistance * 64.0) * envelope * smoothstep(0.02, 0.16, phase);
}

float rippleHeight(vec2 worldXZ) {
    float waves = sin(worldXZ.x * 2.65 + u_Time * 1.55)
                 + sin(worldXZ.y * 3.15 - u_Time * 1.28)
                 + sin((worldXZ.x + worldXZ.y) * 1.72 + u_Time * 0.93);
    waves *= 0.055;

    float rings = cellRipple(worldXZ, 0.44, 0.54, vec2(1.7, 8.2));
    rings += cellRipple(worldXZ, 0.71, 0.67, vec2(-4.3, 2.6)) * 0.58;
    return waves + rings * 0.34;
}

vec3 rippleNormalView(vec2 worldXZ) {
    float epsilon = 0.035;
    float center = rippleHeight(worldXZ);
    float dx = (rippleHeight(worldXZ + vec2(epsilon, 0.0)) - center) / epsilon;
    float dz = (rippleHeight(worldXZ + vec2(0.0, epsilon)) - center) / epsilon;
    float amount = u_RippleStrength * mix(0.35, 1.0, saturate(u_RainAmount));
    vec3 worldNormal = normalize(vec3(-dx * amount, 1.0, -dz * amount));
    return normalize(mat3(u_ViewMatrix) * worldNormal);
}

vec2 projectDirection(vec3 direction, out float valid) {
    vec4 clip = u_ProjectionMatrix * vec4(direction, 0.0);
    if (clip.w <= 0.00001) {
        valid = 0.0;
        return vUv;
    }
    vec2 uv = clip.xy / clip.w * 0.5 + 0.5;
    vec2 inside = step(vec2(0.002), uv) * step(uv, vec2(0.998));
    valid = inside.x * inside.y;
    return safeUv(uv);
}

vec2 projectPosition(vec3 position, out float valid) {
    vec4 clip = u_ProjectionMatrix * vec4(position, 1.0);
    if (clip.w <= 0.00001) {
        valid = 0.0;
        return vUv;
    }
    vec2 uv = clip.xy / clip.w * 0.5 + 0.5;
    vec2 inside = step(vec2(0.002), uv) * step(uv, vec2(0.998));
    valid = inside.x * inside.y;
    return safeUv(uv);
}

vec2 traceReflection(vec3 origin, vec3 rayDirection, vec3 surfaceNormal, out float hitMask) {
    vec3 rayPosition = origin + surfaceNormal * max(0.035, length(origin) * 0.0015);
    float stepLength = max(0.12, length(origin) * 0.014);
    float traveled = 0.0;
    vec2 lastUv = vUv;
    hitMask = 0.0;

    for (int i = 0; i < MAX_REFLECTION_STEPS; i++) {
        if (i >= u_ReflectionSteps) break;

        rayPosition += rayDirection * stepLength;
        traveled += stepLength;
        if (traveled > min(30.0, u_MaxDistance * 0.72) || rayPosition.z > -0.025) break;

        float projected = 0.0;
        vec2 rayUv = projectPosition(rayPosition, projected);
        if (projected < 0.5) break;
        lastUv = rayUv;

        float sceneDepth = texture(u_DepthTexture, rayUv).r;
        if (sceneDepth > 0.000001) {
            vec3 scenePosition = reconstructViewPosition(rayUv, sceneDepth);
            float depthDelta = scenePosition.z - rayPosition.z;
            float heightAboveSurface = dot(scenePosition - origin, surfaceNormal);
            float thickness = 0.07 + stepLength * 1.35;

            if (i > 1 && heightAboveSurface > 0.055
                    && depthDelta > 0.0 && depthDelta < thickness) {
                float edge = min(min(rayUv.x, rayUv.y), min(1.0 - rayUv.x, 1.0 - rayUv.y));
                hitMask = smoothstep(0.0, 0.055, edge)
                        * (1.0 - smoothstep(0.0, thickness, depthDelta) * 0.34);
                return rayUv;
            }
        }

        stepLength *= 1.235;
    }

    return lastUv;
}

vec3 blurredReflection(vec2 uv, float blurPixels) {
    vec2 texel = 1.0 / max(u_Resolution, vec2(1.0));
    vec2 horizontal = vec2(texel.x * blurPixels, 0.0);
    vec2 vertical = vec2(0.0, texel.y * blurPixels * 1.85);

    vec3 color = texture(u_SceneTexture, safeUv(uv)).rgb * 0.34;
    color += texture(u_SceneTexture, safeUv(uv + horizontal)).rgb * 0.12;
    color += texture(u_SceneTexture, safeUv(uv - horizontal)).rgb * 0.12;
    color += texture(u_SceneTexture, safeUv(uv + vertical)).rgb * 0.16;
    color += texture(u_SceneTexture, safeUv(uv - vertical)).rgb * 0.16;
    color += texture(u_SceneTexture, safeUv(uv + vertical * 2.15)).rgb * 0.05;
    color += texture(u_SceneTexture, safeUv(uv - vertical * 2.15)).rgb * 0.05;
    return color;
}

void main() {
    vec4 original = texture(u_SceneTexture, vUv);
    float depth = texture(u_DepthTexture, vUv).r;
    if (depth <= 0.000001) {
        fragColor = original;
        return;
    }

    vec3 viewPosition = reconstructViewPosition(vUv, depth);
    float distanceToCamera = length(viewPosition);
    if (distanceToCamera > u_MaxDistance || distanceToCamera < 0.32) {
        fragColor = original;
        return;
    }

    vec3 viewNormal = reconstructViewNormal(vUv, viewPosition);
    vec3 worldNormal = normalize(mat3(u_InverseViewMatrix) * viewNormal);

    // The normal is oriented toward the camera. A floor below the camera has
    // positive world Y; a ceiling above it has negative world Y. Do not use
    // abs() here: that would make ceilings reflective too.
    float upwardMask = smoothstep(0.58, 0.88, worldNormal.y);
    if (upwardMask <= 0.002) {
        fragColor = original;
        return;
    }

    vec3 worldPosition = reconstructWorldPosition(viewPosition);
    float macroNoise = puddleNoise(worldPosition.xz * 0.067);
    float detailNoise = valueNoise(worldPosition.xz * 0.285 + vec2(19.4, -7.8));
    float puddleField = macroNoise * 0.78 + detailNoise * 0.22;
    float threshold = mix(0.73, 0.27, saturate(u_PuddleCoverage));
    float puddle = u_PuddleCoverage <= 0.001 ? 0.0
            : smoothstep(threshold - 0.095, threshold + 0.075, puddleField);

    float rangeFade = 1.0 - smoothstep(u_MaxDistance * 0.72, u_MaxDistance, distanceToCamera);
    float nearFade = smoothstep(0.32, 0.82, distanceToCamera);
    float wetFilm = 0.44 + puddle * 0.56;
    float wetMask = saturate(upwardMask * rangeFade * nearFade * wetFilm * u_Wetness);
    if (wetMask <= 0.002) {
        fragColor = original;
        return;
    }

    vec3 reflectionNormal = viewNormal;
    float rippleHighlight = 0.0;
    if (u_Ripples == 1 && u_RippleStrength > 0.001) {
        vec3 rippled = rippleNormalView(worldPosition.xz);
        float rippleMix = saturate(puddle * 0.70 * u_RippleStrength);
        reflectionNormal = normalize(mix(viewNormal, rippled, rippleMix));
        rippleHighlight = max(rippleHeight(worldPosition.xz), 0.0) * saturate(u_RippleStrength) * puddle;
    }

    vec3 incident = normalize(viewPosition);
    vec3 reflectedDirection = normalize(reflect(incident, reflectionNormal));

    float rayHit = 0.0;
    vec2 rayUv = traceReflection(viewPosition, reflectedDirection, viewNormal, rayHit);
    // A missed ray must not sample an arbitrary object at the projected
    // infinite direction. Use a world-oriented sky approximation instead;
    // only a traced hit can contribute geometry from the scene.
    vec3 worldDirection = normalize(mat3(u_InverseViewMatrix) * reflectedDirection);
    vec3 reflectedColor = mix(SkyColor.rgb * 0.35,
            SkyColor.rgb * 0.86 + vec3(0.10, 0.12, 0.15), saturate(worldDirection.y * 0.7 + 0.3));
    float roughness = mix(3.4, 1.35, puddle);
    if (rayHit > 0.001) reflectedColor = mix(reflectedColor, blurredReflection(rayUv, roughness), rayHit);
    reflectedColor = mix(reflectedColor, reflectedColor * vec3(0.93, 0.98, 1.08), 0.34);

    vec3 viewToCamera = normalize(-viewPosition);
    float noV = saturate(dot(reflectionNormal, viewToCamera));
    float fresnel = 0.12 + 0.88 * pow(1.0 - noV, 3.0);

    float darkening = saturate(wetMask * (0.20 + puddle * 0.16));
    vec3 wetBase = original.rgb * (1.0 - darkening);
    float wetLuma = luminance(wetBase);
    wetBase = mix(vec3(wetLuma), wetBase, 1.12);
    wetBase *= vec3(0.96, 0.985, 1.035);

    float reflectivePuddle = smoothstep(0.30, 0.72, puddle);
    float reflectionAmount = wetMask
            * reflectivePuddle
            * (0.30 + fresnel * 0.82)
            * u_ReflectionStrength;
    reflectionAmount = min(reflectionAmount, 0.90);

    vec3 color = mix(original.rgb, wetBase, saturate(wetMask * 0.92));
    color = mix(color, reflectedColor * 1.06, reflectionAmount);

    float reflectedLight = smoothstep(0.58, 1.0, luminance(reflectedColor));
    color += reflectedColor * reflectedLight * wetMask * (0.025 + fresnel * 0.09) * u_ReflectionStrength;
    color += vec3(0.035, 0.052, 0.075) * fresnel * wetMask * (0.30 + puddle * 0.70) * u_ReflectionStrength;
    color += vec3(0.18, 0.22, 0.28) * rippleHighlight * wetMask * 0.035;

    fragColor = vec4(clamp(color, 0.0, 1.0), original.a);
}
