#version 150

layout(std140) uniform FogAtmosphere {
    mat4 InverseProjection;
    mat4 InverseView;
    mat4 CleanInverseProjection;
    mat4 CleanInverseView;
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

in vec2 TexCoord;

out vec4 fragColor;

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
