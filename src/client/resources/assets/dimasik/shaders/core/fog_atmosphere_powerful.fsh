#version 330
#extension GL_ARB_separate_shader_objects : require
#define DIMASIK_DEPTH_TO_CLIP(depth) (depth)

layout(std140) uniform PowerfulAtmosphere {
    mat4 InverseProjection;
    mat4 InverseView;
    vec4 CameraData;
    vec4 SunDirection;
    vec4 SunScreen;
    vec4 FogData;
    vec4 ScreenData;
    vec4 EffectData;
    vec4 PaletteZenith;
    vec4 PaletteHorizonWarm;
    vec4 PaletteHorizonCool;
    vec4 PaletteFogWarm;
    vec4 PaletteFogCool;
    vec4 PaletteRay;
};

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;

layout(location = 0) in vec2 TexCoord;

layout(location = 0) out vec4 fragColor;

const vec2 POISSON[8] = vec2[8](
    vec2(-0.326, -0.406), vec2(-0.840, -0.074), vec2(-0.696, 0.457), vec2(-0.203, 0.621),
    vec2(0.962, -0.195), vec2(0.473, -0.480), vec2(0.519, 0.767), vec2(0.185, -0.893)
);

float saturate(float value) {
    return clamp(value, 0.0, 1.0);
}

float luminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

float hash12(vec2 value) {
    vec3 value3 = fract(vec3(value.xyx) * 0.1031);
    value3 += dot(value3, value3.yzx + vec3(33.33));
    return fract((value3.x + value3.y) * value3.z);
}

vec3 srgbToLinear(vec3 color) {
    return vec3(
        color.r <= 0.04045 ? color.r / 12.92 : pow((color.r + 0.055) / 1.055, 2.4),
        color.g <= 0.04045 ? color.g / 12.92 : pow((color.g + 0.055) / 1.055, 2.4),
        color.b <= 0.04045 ? color.b / 12.92 : pow((color.b + 0.055) / 1.055, 2.4)
    );
}

vec3 linearToSrgb(vec3 color) {
    color = clamp(color, 0.0, 1.0);
    return vec3(
        color.r <= 0.0031308 ? color.r * 12.92 : 1.055 * pow(color.r, 0.4166666666666667) - 0.055,
        color.g <= 0.0031308 ? color.g * 12.92 : 1.055 * pow(color.g, 0.4166666666666667) - 0.055,
        color.b <= 0.0031308 ? color.b * 12.92 : 1.055 * pow(color.b, 0.4166666666666667) - 0.055
    );
}

vec3 linearToOklab(vec3 color) {
    float l = 0.4122214708 * color.r + 0.5363325363 * color.g + 0.0514459929 * color.b;
    float m = 0.2119034982 * color.r + 0.6806995451 * color.g + 0.1073969566 * color.b;
    float s = 0.0883024619 * color.r + 0.2817188376 * color.g + 0.6299787005 * color.b;
    float lRoot = sign(l) * pow(abs(l), 0.3333333333333333);
    float mRoot = sign(m) * pow(abs(m), 0.3333333333333333);
    float sRoot = sign(s) * pow(abs(s), 0.3333333333333333);
    return vec3(
        0.2104542553 * lRoot + 0.7936177850 * mRoot - 0.0040720468 * sRoot,
        1.9779984951 * lRoot - 2.4285922050 * mRoot + 0.4505937099 * sRoot,
        0.0259040371 * lRoot + 0.7827717662 * mRoot - 0.8086757660 * sRoot
    );
}

vec3 oklabToLinear(vec3 color) {
    float lRoot = color.x + 0.3963377774 * color.y + 0.2158037573 * color.z;
    float mRoot = color.x - 0.1055613458 * color.y - 0.0638541728 * color.z;
    float sRoot = color.x - 0.0894841775 * color.y - 1.2914855480 * color.z;
    float l = lRoot * lRoot * lRoot;
    float m = mRoot * mRoot * mRoot;
    float s = sRoot * sRoot * sRoot;
    return vec3(
        4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
        -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
        -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
    );
}

vec3 oklabMix(vec3 colorA, vec3 colorB, float progress) {
    vec3 labA = linearToOklab(srgbToLinear(colorA));
    vec3 labB = linearToOklab(srgbToLinear(colorB));
    return linearToSrgb(oklabToLinear(mix(labA, labB, saturate(progress))));
}

vec4 permute4(vec4 value) {
    return mod(((value * 34.0) + 1.0) * value, 289.0);
}

vec4 taylorInvSqrt4(vec4 value) {
    return 1.79284291400159 - 0.85373472095314 * value;
}

float snoise(vec3 value) {
    const vec2 C = vec2(1.0 / 6.0, 1.0 / 3.0);
    const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);
    vec3 cell = floor(value + dot(value, C.yyy));
    vec3 x0 = value - cell + dot(cell, C.xxx);
    vec3 g = step(x0.yzx, x0.xyz);
    vec3 lower = 1.0 - g;
    vec3 i1 = min(g.xyz, lower.zxy);
    vec3 i2 = max(g.xyz, lower.zxy);
    vec3 x1 = x0 - i1 + C.xxx;
    vec3 x2 = x0 - i2 + 2.0 * C.xxx;
    vec3 x3 = x0 - 1.0 + 3.0 * C.xxx;
    cell = mod(cell, 289.0);
    vec4 permutation = permute4(permute4(permute4(
        cell.z + vec4(0.0, i1.z, i2.z, 1.0))
        + cell.y + vec4(0.0, i1.y, i2.y, 1.0))
        + cell.x + vec4(0.0, i1.x, i2.x, 1.0));
    float seventh = 1.0 / 7.0;
    vec3 ns = seventh * D.wyz - D.xzx;
    vec4 j = permutation - 49.0 * floor(permutation * ns.z * ns.z);
    vec4 xFloor = floor(j * ns.z);
    vec4 yFloor = floor(j - 7.0 * xFloor);
    vec4 x = xFloor * ns.x + ns.yyyy;
    vec4 y = yFloor * ns.x + ns.yyyy;
    vec4 h = 1.0 - abs(x) - abs(y);
    vec4 b0 = vec4(x.xy, y.xy);
    vec4 b1 = vec4(x.zw, y.zw);
    vec4 s0 = floor(b0) * 2.0 + 1.0;
    vec4 s1 = floor(b1) * 2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));
    vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;
    vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;
    vec3 p0 = vec3(a0.xy, h.x);
    vec3 p1 = vec3(a0.zw, h.y);
    vec3 p2 = vec3(a1.xy, h.z);
    vec3 p3 = vec3(a1.zw, h.w);
    vec4 norm = taylorInvSqrt4(vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3)));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;
    vec4 influence = max(0.6 - vec4(dot(x0, x0), dot(x1, x1), dot(x2, x2), dot(x3, x3)), 0.0);
    influence *= influence;
    return 42.0 * dot(influence * influence, vec4(dot(p0, x0), dot(p1, x1), dot(p2, x2), dot(p3, x3)));
}

float fbm(vec3 point) {
    float sum = 0.6 * snoise(point);
    sum += 0.3 * snoise(point * 2.11 + vec3(19.1, 7.3, 5.2));
    return sum;
}

vec3 worldPosFromDepth(vec2 uv, float depth) {
    vec4 clipSpace = vec4(uv * 2.0 - 1.0, DIMASIK_DEPTH_TO_CLIP(depth), 1.0);
    vec4 viewSpace = InverseProjection * clipSpace;
    float inverseW = abs(viewSpace.w) > 0.000001 ? 1.0 / viewSpace.w : 1.0;
    viewSpace *= inverseW;
    return (InverseView * viewSpace).xyz;
}

float heightIntegral(vec3 origin, vec3 direction, float distance) {
    float thickness = max(FogData.z - FogData.y, 1.0);
    float falloff = 2.0 / thickness;
    float directionY = abs(direction.y) < 0.00002 ? (direction.y < 0.0 ? -0.00002 : 0.00002) : direction.y;
    float base = exp(-clamp(falloff * max(origin.y - FogData.y, 0.0), 0.0, 60.0));
    float travel = 1.0 - exp(clamp(-falloff * directionY * distance, -60.0, 60.0));
    return clamp(base * travel / (falloff * directionY), 0.0, distance);
}

float henyeyGreenstein(float cosine, float anisotropy) {
    float anisotropySquared = anisotropy * anisotropy;
    return (1.0 - anisotropySquared) / pow(max(1.0 + anisotropySquared - 2.0 * anisotropy * cosine, 0.0001), 1.5);
}

float godRaysMask(vec2 uv, vec2 sunUv, float jitter) {
    vec2 stepUv = (sunUv - uv) / 16.0;
    vec2 position = uv + stepUv * jitter;
    float sum = 0.0;
    float weight = 1.0;
    float total = 0.0;
    for (int index = 0; index < 16; index++) {
        vec2 sampleUv = clamp(position, vec2(0.001), vec2(0.999));
        sum += (1.0 - step(0.000001, texture(DepthSampler, sampleUv).r)) * weight;
        total += weight;
        weight *= 0.925;
        position += stepUv;
    }
    return sum / max(total, 0.0001);
}

vec3 sampleSceneSoft(vec2 uv, float radiusPixels, float rotation) {
    vec3 center = texture(SceneSampler, uv).rgb;
    if (radiusPixels < 0.5) {
        return center;
    }
    vec2 pixel = vec2(radiusPixels) / max(ScreenData.xy, vec2(1.0));
    float cosine = cos(rotation);
    float sine = sin(rotation);
    mat2 basis = mat2(cosine, sine, -sine, cosine);
    vec3 accumulated = center * 0.2;
    for (int index = 0; index < 8; index++) {
        vec2 sampleUv = clamp(uv + basis * POISSON[index] * pixel, vec2(0.001), vec2(0.999));
        accumulated += texture(SceneSampler, sampleUv).rgb * 0.1;
    }
    return accumulated;
}

vec3 atmosphericSky(vec3 direction, float sunAlignment) {
    float up = saturate(direction.y);
    float below = saturate(-direction.y);
    vec3 gradient = oklabMix(PaletteHorizonCool.rgb, PaletteZenith.rgb, pow(up, 0.58));
    float band = pow(1.0 - up, 3.2);
    float sunNear = pow(saturate(sunAlignment * 0.5 + 0.5), 3.0);
    gradient = oklabMix(gradient, PaletteHorizonWarm.rgb, saturate(sunNear * (0.30 + 0.70 * band)));
    gradient += PaletteRay.rgb * henyeyGreenstein(sunAlignment, 0.62) * 0.045;
    gradient = mix(gradient, PaletteHorizonCool.rgb * 0.9, below * 0.8);
    return gradient;
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(ScreenData.xy, vec2(1.0));
    float depth = texture(DepthSampler, uv).r;
    float validDepth = step(0.000001, depth);
    float skyMask = 1.0 - validDepth;

    vec3 origin = CameraData.xyz;
    vec3 farPoint = worldPosFromDepth(uv, 0.000001);
    vec3 direction = normalize(farPoint - origin + vec3(0.0, 0.0000001, 0.0));
    vec3 surfacePosition = worldPosFromDepth(uv, max(depth, 0.000001));
    float sceneDistance = mix(2600.0, length(surfacePosition - origin), validDepth);

    float edgeFade = smoothstep(0.0, 7.5, sceneDistance);
    float middleY = (FogData.y + FogData.z) * 0.5;
    float toMiddle = abs(middleY - origin.y) / max(abs(direction.y), 0.04);
    float probeDistance = clamp(min(toMiddle, sceneDistance * 0.5), 6.0, 220.0);
    float caveFade = smoothstep(FogData.y - 40.0, FogData.y - 12.0, origin.y);
    float densityModifier = 1.0;
    if (caveFade > 0.0001) {
        vec3 probe = origin + direction * probeDistance;
        vec3 drift = vec3(ScreenData.z * 1.05, ScreenData.z * 0.16, ScreenData.z * 0.58);
        float macro = fbm(probe * 0.045 + drift * 0.6);
        float sheets = snoise(vec3(probe.x * 0.052, probe.y * 0.155, probe.z * 0.052) + drift * 0.35);
        vec3 nearProbe = origin + direction * min(probeDistance * 0.45, 60.0);
        float detail = snoise(nearProbe * 0.21 - drift * 0.5);
        float turbulence = macro * 0.62 + sheets * 0.28 + detail * 0.10;
        densityModifier = mix(0.55, 1.50, saturate(turbulence * 0.5 + 0.5));
        densityModifier = mix(densityModifier, 1.0, smoothstep(120.0, 220.0, probeDistance));
    }
    float sigma = FogData.x * 0.058 * densityModifier * caveFade;
    float geometryFog = saturate((1.0 - exp(-sigma * heightIntegral(origin, direction, min(sceneDistance, 900.0)))) * edgeFade);
    float skyFog = saturate(1.0 - exp(-sigma * heightIntegral(origin, direction, 4000.0)));
    float fogAmount = mix(skyFog, geometryFog, validDepth);

    vec3 sun = normalize(SunDirection.xyz);
    float sunAlignment = dot(direction, sun);
    float lowSun = saturate(1.0 - abs(sun.y) * 2.2);
    float horizonGlow = pow(saturate(1.0 - abs(direction.y) * 1.30), 2.3);
    vec3 skyColor = atmosphericSky(direction, sunAlignment);

    float sunward = pow(saturate(sunAlignment * 0.5 + 0.5), 3.4);
    float vertical = saturate(direction.y * 1.9 + 0.55);
    vec3 shadowSide = mix(PaletteFogCool.rgb * 0.92, PaletteFogCool.rgb * 1.10, vertical);
    vec3 dawnSide = mix(PaletteFogWarm.rgb, PaletteHorizonWarm.rgb, pow(saturate(sunAlignment), 2.4));
    float bleed = saturate(sunward * (0.30 + 0.70 * lowSun));
    vec3 fogTint = oklabMix(shadowSide, dawnSide, bleed);
    float forward = henyeyGreenstein(sunAlignment, 0.56) * 0.085;
    vec3 bloomGlow = PaletteRay.rgb * forward * (0.35 + 0.65 * lowSun) * horizonGlow;
    fogTint += bloomGlow * 0.85;
    float tintLuminance = luminance(fogTint);
    fogTint /= 1.0 + max(tintLuminance - 0.865, 0.0) * 2.4;

    float viewRange = max(FogData.w, 64.0);
    float farHaze = smoothstep(viewRange * 0.45, viewRange * 0.92, sceneDistance) * validDepth;
    farHaze *= mix(0.75, 1.0, pow(1.0 - saturate(direction.y), 3.2));
    float skyBlend = saturate(0.42 + 0.58 * pow(1.0 - saturate(direction.y), 2.6));

    float blurDrive = max(fogAmount, max(farHaze * 0.7, skyMask * skyBlend * 0.45));
    float blurRadius = ScreenData.w * 7.5 * blurDrive;
    float rotation = hash12(gl_FragCoord.xy) * 6.2831853;
    vec3 scene = sampleSceneSoft(uv, blurRadius, rotation);

    vec3 color = mix(scene, skyColor, skyMask * skyBlend);
    vec3 veiled = color * mix(vec3(1.0), vec3(0.90, 0.94, 1.03), fogAmount * 0.30);
    color = mix(veiled, fogTint, fogAmount);
    color = mix(color, skyColor, farHaze * 0.92);

    float rays = 0.0;
    vec3 sunGlow = vec3(0.0);
    if (EffectData.x > 0.001 && SunScreen.z > 0.001) {
        float jitter = hash12(gl_FragCoord.yx + vec2(17.0, 59.0));
        float mask = godRaysMask(uv, SunScreen.xy, jitter);
        float angular = pow(saturate(sunAlignment * 0.5 + 0.5), 3.0);
        rays = mask * angular * SunScreen.z * EffectData.x;
        rays *= 0.45 + 0.55 * saturate(fogAmount + skyMask * 0.85 + farHaze);
        float deltaY = direction.y - sun.y;
        float angularSquared = max(2.0 * (1.0 - sunAlignment), 0.0);
        float disc = exp(-angularSquared * 2600.0);
        float deltaX2 = max(angularSquared - deltaY * deltaY, 0.0);
        float streak = exp(-(deltaY * deltaY * 850.0 + deltaX2 * 34.0));
        float glowVisibility = SunScreen.z * EffectData.x * (0.35 + 0.65 * lowSun);
        float sunCanvas = saturate(skyMask + farHaze);
        sunGlow = PaletteRay.rgb * (disc * 1.05 * sunCanvas + streak * 0.38 * mix(0.25, 1.0, sunCanvas)) * glowVisibility;
    }
    vec3 rayGlow = clamp(PaletteRay.rgb * rays * 0.85 + sunGlow, vec3(0.0), vec3(1.0));
    color = 1.0 - (1.0 - clamp(color, vec3(0.0), vec3(1.0))) * (1.0 - rayGlow);

    float outputLuminance = luminance(color);
    color /= 1.0 + max(outputLuminance - 0.985, 0.0) * 1.6;
    color += vec3((hash12(gl_FragCoord.xy + vec2(311.7, 74.3)) - 0.5) * 0.0078);
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
