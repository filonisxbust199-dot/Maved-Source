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

vec3 rayColor(float isDay) {
    return mix(vec3(0.38, 0.56, 1.0), DawnColor.rgb, isDay);
}

void main() {
    vec4 atmosphere = texture(FogSampler, TexCoord);
    float isDay = LightingParams.w;
    float nightMistMode = 1.0 - step(0.45, abs(LightingParams.z - 1.0));
    vec3 rays = rayColor(isDay)
        * atmosphere.g * mix(0.68, 0.98, isDay)
        * mix(1.0, 0.42, nightMistMode);
    fragColor = vec4(clamp(rays, 0.0, 1.0), 0.0);
}
