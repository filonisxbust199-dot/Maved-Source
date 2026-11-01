#version 330
#extension GL_ARB_separate_shader_objects : require
#define DIMASIK_DEPTH_TO_CLIP(depth) (depth)

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

uniform sampler2D DepthSampler;

layout(location = 0) in vec2 TexCoord;

layout(location = 0) out vec4 fragColor;

const float TAU = 6.28318530718;
const float WORLD_WRAP = TAU / 8192.0;

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + vec3(33.33));
    return fract((p3.x + p3.y) * p3.z);
}

float godRaysMask(vec2 uv, vec2 sunUv, float jitter) {
    vec2 stepUv = (sunUv - uv) / 16.0;
    vec2 pos = uv + stepUv * jitter;
    float sum = 0.0;
    float weight = 1.0;
    float total = 0.0;
    for (int i = 0; i < 16; i++) {
        vec2 s = clamp(pos, vec2(0.001), vec2(0.999));
        sum += (1.0 - step(0.00005, texture(DepthSampler, s).r)) * weight;
        total += weight;
        weight *= 0.925;
        pos += stepUv;
    }
    return sum / max(total, 0.0001);
}

vec3 reconstructViewPosition(vec2 uv, float rawDepth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, DIMASIK_DEPTH_TO_CLIP(rawDepth), 1.0);
    vec4 view = InverseProjection * clip;
    return view.xyz / max(abs(view.w), 1.0e-5);
}

float integratedHeightDensity(float cameraHeight, float directionY, float distance, float scale) {
    float startHeight = max(cameraHeight, 0.0);
    float startDensity = exp(-startHeight / scale);
    if (distance <= 0.001 || abs(directionY) < 0.0001) return startDensity;
    float endHeight = startHeight + directionY * distance;
    if (directionY < 0.0 && endHeight < 0.0) {
        float aboveDistance = clamp(startHeight / -directionY, 0.0, distance);
        float aboveIntegral = scale * (startDensity - 1.0) / directionY;
        return clamp((aboveIntegral + distance - aboveDistance) / distance, 0.0, 1.0);
    }
    float endDensity = exp(-max(endHeight, 0.0) / scale);
    return clamp(scale * (startDensity - endDensity) / (directionY * distance), 0.0, 1.0);
}

float fogStructure(vec3 offset, float time) {
    vec3 p = (CameraFogBase.xyz + offset) * WORLD_WRAP;
    float drift = time * 0.0024;
    float broad = sin(p.x * 31.0 + p.z * 17.0 + drift)
        * sin(p.z * 23.0 - p.x * 11.0 - drift * 0.73);
    float sheets = sin(p.x * 67.0 + p.z * 43.0 + p.y * 9.0 + drift * 1.31);
    float detail = sin(p.x * 113.0 - p.z * 79.0 + p.y * 17.0 - drift * 1.77);
    return clamp(0.82 + broad * 0.17 + sheets * 0.08 + detail * 0.035, 0.56, 1.28);
}

float worldBeamField(vec3 offset, vec3 lightDirection, float softness, float time) {
    vec3 axis = normalize(vec3(lightDirection.y, -lightDirection.x, 0.0));
    vec3 p = CameraFogBase.xyz + offset;
    float across = dot(p, axis) * WORLD_WRAP;
    float depth = p.z * WORLD_WRAP;
    float soft = clamp(softness / 1.5, 0.0, 1.0);
    float drift = time * 0.0014;
    float broad = 0.5 + 0.5 * sin(across * 37.0 + sin(depth * 19.0 - drift) * 1.4 + drift);
    float fine = 0.5 + 0.5 * sin(across * 83.0 - depth * 29.0 - drift * 1.8);
    float broadShaft = pow(broad, mix(6.2, 2.4, soft));
    float fineShaft = pow(fine, mix(10.0, 3.8, soft));
    return clamp(broadShaft * 0.76 + fineShaft * 0.24, 0.0, 1.0);
}

void main() {
    float rawDepth = texture(DepthSampler, TexCoord).r;
    bool isSky = rawDepth <= 0.00005;
    vec3 farViewPosition = reconstructViewPosition(TexCoord, 0.9999);
    vec3 viewDirection = normalize(farViewPosition);
    vec3 worldDirection = normalize((InverseView * vec4(viewDirection, 0.0)).xyz);
    vec3 lightDirection = normalize(SunData.xyz);

    float renderDistance = max(ScreenParams.w, 48.0);
    float sceneDistance = isSky
        ? renderDistance
        : min(length(reconstructViewPosition(TexCoord, rawDepth)), renderDistance);
    float densityControl = clamp(FogParams.x, 0.05, 1.5);
    float nightMistMode = 1.0 - step(0.45, abs(LightingParams.z - 1.0));
    float density = pow(densityControl, 0.76) * 1.34;
    density *= mix(1.0, 0.86, nightMistMode);
    float layerHeight = max(FogParams.y, 1.0);
    layerHeight *= mix(1.0, 0.72, nightMistMode);
    float softness = clamp(FogParams.z, 0.1, 1.5);
    float rayStrength = max(FogParams.w, 0.0);
    float nearClear = mix(2.8, 0.55, clamp(densityControl / 1.5, 0.0, 1.0));
    float fogDistance = max(0.0, sceneDistance - nearClear);
    if (fogDistance <= 0.001) {
        fragColor = vec4(0.0);
        return;
    }

    float verticalDepth = max(4.0, 2.0 + layerHeight * (0.88 + softness * 0.76));
    float cameraHeight = max(CameraFogBase.y - CameraFogBase.w, 0.0);
    float heightDensity = integratedHeightDensity(cameraHeight, worldDirection.y, fogDistance, verticalDepth);
    float altitudeVeil = exp(-cameraHeight / max(verticalDepth * 2.7, 1.0))
        * mix(0.06, 0.15, densityControl / 1.5);
    heightDensity = max(heightDensity, altitudeVeil);

    float sampleDistance = nearClear + min(fogDistance, 150.0) * 0.56;
    vec3 sampleOffset = worldDirection * sampleDistance;
    float structure = fogStructure(sampleOffset, ScreenParams.z);
    float opticalDepth = density * heightDensity * structure * fogDistance * 0.054;
    float fogAlpha = 1.0 - exp(-opticalDepth);
    float horizon = pow(max(0.0, 1.0 - abs(worldDirection.y)), mix(7.2, 3.2, softness / 1.5));
    if (isSky) {
        float altitudeVisibility = exp(-cameraHeight / max(verticalDepth * 2.8, 1.0));
        float horizonFog = horizon * (1.0 - exp(-density * 1.08)) * mix(0.16, 1.0, altitudeVisibility);
        fogAlpha = 1.0 - (1.0 - fogAlpha) * (1.0 - horizonFog);
    }
    fogAlpha = clamp(fogAlpha, 0.0, 0.965);

    float localGlow = 0.0;
    if (rayStrength > 0.001) {
        float alignment = dot(worldDirection, lightDirection);
        float forwardScatter = pow(max(alignment, 0.0), mix(8.0, 3.2, softness / 1.5));
        float backwardScatter = pow(max(-alignment, 0.0), 3.0);
        float scatter = 0.38 + forwardScatter * 0.50 + backwardScatter * 0.12;
        float moisture = mix(0.18, 1.0, smoothstep(0.025, 0.76, fogAlpha));
        float shaftDistance = mix(0.16, 1.0, smoothstep(3.0, 44.0, fogDistance));
        vec3 nearOffset = worldDirection * (nearClear + min(fogDistance, 130.0) * 0.26);
        vec3 farOffset = worldDirection * (nearClear + min(fogDistance, 130.0) * 0.78);
        float beams = mix(
            worldBeamField(nearOffset, lightDirection, softness, ScreenParams.z),
            worldBeamField(farOffset, lightDirection, softness, ScreenParams.z),
            0.58
        );
        float worldRays = beams * shaftDistance * moisture * SunData.w * rayStrength
            * mix(0.46, 0.96, scatter);

        if (SunScreen.w > 0.5) {
            float jitter = hash12(gl_FragCoord.yx + vec2(17.0, 59.0));
            float occlusion = godRaysMask(TexCoord, SunScreen.xy, jitter);
            float shadowBlend = clamp(SunScreen.z * 1.6, 0.0, 1.0);
            worldRays *= mix(1.0, mix(0.06, 1.0, occlusion), shadowBlend);
        }

        localGlow = clamp(worldRays, 0.0, 0.92);
    }

    fragColor = vec4(fogAlpha, localGlow, isSky ? 1.0 : 0.0, 0.0);
}
