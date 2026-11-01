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

vec3 worldPositionFromDepth(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, DIMASIK_DEPTH_TO_CLIP(depth), 1.0);
    vec4 view = InverseProjection * clip;
    view /= max(abs(view.w), 0.000001);
    return (InverseView * view).xyz;
}

float heightIntegral(vec3 origin, vec3 direction, float distance) {
    float thickness = max(FogData.z - FogData.y, 1.0);
    float falloff = 2.0 / thickness;
    float directionY = abs(direction.y) < 0.00002
            ? (direction.y < 0.0 ? -0.00002 : 0.00002)
            : direction.y;
    float base = exp(-clamp(falloff * max(origin.y - FogData.y, 0.0), 0.0, 60.0));
    float travel = 1.0 - exp(clamp(-falloff * directionY * distance, -60.0, 60.0));
    return clamp(base * travel / (falloff * directionY), 0.0, distance);
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(ScreenData.xy, vec2(1.0));
    vec3 scene = texture(SceneSampler, uv).rgb;
    float depth = texture(DepthSampler, uv).r;
    bool sky = depth <= 0.000001;
    vec3 origin = CameraData.xyz;
    vec3 farPosition = worldPositionFromDepth(uv, 0.000001);
    vec3 direction = normalize(farPosition - origin + vec3(0.0, 0.0000001, 0.0));
    vec3 surfacePosition = worldPositionFromDepth(uv, max(depth, 0.000001));
    float distanceToSurface = sky
            ? max(FogData.w, 64.0) * 2.0
            : min(length(surfacePosition - origin), max(FogData.w, 64.0));
    float nearClear = smoothstep(2.0, 8.0, distanceToSurface);
    float opticalDepth = FogData.x * 0.058
            * heightIntegral(origin, direction, distanceToSurface)
            * nearClear;
    float fogAmount = clamp(1.0 - exp(-opticalDepth), 0.0, 0.965);
    float sunSide = clamp(dot(direction, normalize(SunDirection.xyz)) * 0.5 + 0.5, 0.0, 1.0);
    vec3 fogColor = mix(PaletteFogCool.rgb, PaletteFogWarm.rgb, sunSide * 0.28);
    fragColor = vec4(mix(scene, fogColor, fogAmount), 1.0);
}
