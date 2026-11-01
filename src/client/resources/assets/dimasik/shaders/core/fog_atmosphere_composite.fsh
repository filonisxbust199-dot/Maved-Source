#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform FogAtmosphere {
    mat4 InverseProjection;
    mat4 InverseView;
    vec4 CameraFogBase;
    vec4 SunData;
    vec4 SunScreen;
    vec4 LightingParams;
    vec4 FogParams;
    vec4 ScreenParams;
    vec4 FogColor;
    vec4 DawnColor;
};

uniform sampler2D FogSampler;

layout(location = 0) in vec2 TexCoord;

layout(location = 0) out vec4 fragColor;

vec3 paletteColor(float lowSun, float isDay) {
    vec3 cold = FogColor.rgb * 0.86;
    vec3 dawn = DawnColor.rgb;
    vec3 night = mix(cold * 0.48, vec3(0.20, 0.29, 0.49), 0.72);
    return mix(night, mix(cold, dawn * 0.72, lowSun * 0.34), isDay);
}

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float modeMask(float mode, float selected) {
    return 1.0 - step(0.45, abs(mode - selected));
}

void main() {
    vec4 atmosphere = texture(FogSampler, TexCoord);
    float fogAlpha = atmosphere.r;
    float extraEffects = step(-0.5, SunScreen.w);
    float isDay = LightingParams.w;
    float atmosphereMode = LightingParams.z;
    float nightMistMode = modeMask(atmosphereMode, 1.0);
    float lowSun = (1.0 - smoothstep(0.16, 0.72, max(SunData.y, 0.0))) * SunData.w * isDay;
    vec3 color = paletteColor(lowSun, isDay);
    vec3 nightColor = mix(vec3(0.035, 0.075, 0.15), vec3(0.13, 0.22, 0.39), fogAlpha);
    color = mix(color, nightColor, nightMistMode);
    float luminousMist = atmosphere.g * mix(0.12, 0.28, isDay) * extraEffects;
    vec3 rayTint = mix(vec3(0.38, 0.56, 1.0), DawnColor.rgb, isDay);
    color = mix(color, rayTint, clamp(luminousMist, 0.0, 0.32));

    vec2 starGrid = floor(TexCoord * vec2(210.0, 118.0));
    vec2 starCell = fract(TexCoord * vec2(210.0, 118.0)) - 0.5;
    float starSeed = hash12(starGrid);
    float star = step(0.991, starSeed)
        * smoothstep(0.10, 0.0, length(starCell))
        * (0.35 + 0.65 * sin(ScreenParams.z * 1.25 + starSeed * 31.0))
        * atmosphere.b
        * extraEffects
        * smoothstep(0.42, 0.78, TexCoord.y);
    color += vec3(0.24, 0.42, 0.78) * star * nightMistMode * 0.38;

    fragColor = vec4(clamp(color, 0.0, 1.0), fogAlpha);
}
