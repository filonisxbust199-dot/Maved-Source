#version 330
#extension GL_ARB_separate_shader_objects : require
#define DIMASIK_DEPTH_TO_CLIP(depth) (depth)

const int MAX_IMPACTS = 6;

layout(std140) uniform HitSonnar {
    mat4 InverseProjection;
    mat4 InverseView;
    vec4 CameraTime;
    vec4 Params;
    vec4 Color;
    vec4 Impacts[MAX_IMPACTS];
    vec4 Frames[MAX_IMPACTS];
};

uniform sampler2D DepthSampler;

layout(location = 0) in vec2 TexCoord;
layout(location = 0) out vec4 fragColor;

vec3 viewFromDepth(vec2 uv, float depth) {
    vec4 homogeneous = InverseProjection * vec4(uv * 2.0 - 1.0, DIMASIK_DEPTH_TO_CLIP(depth), 1.0);
    return homogeneous.xyz / homogeneous.w;
}

float sonarEnergy(vec3 relativeWorld) {
    int count = min(int(Params.x + 0.5), MAX_IMPACTS);
    float energy = 0.0;

    for (int index = 0; index < count; index++) {
        vec4 impact = Impacts[index];
        vec4 frame = Frames[index];
        if (frame.x <= 0.002) continue;

        vec2 fromImpact = relativeWorld.xz - impact.xz;
        float horizontalDistance = length(fromImpact);
        if (horizontalDistance > Params.w) continue;
        float frontRadius = impact.w * Params.w;
        float offset = frontRadius - horizontalDistance;
        float coreWidth = clamp(Params.w * 0.025, 0.7, 2.2);
        float trailWidth = clamp(Params.y * Params.w, 2.5, 12.0);
        float core = 1.0 - smoothstep(coreWidth * 0.25, coreWidth, abs(offset));
        float trail = smoothstep(-coreWidth, 0.0, offset)
            * (1.0 - smoothstep(coreWidth * 0.35, trailWidth, offset));
        float leadingGlow = smoothstep(-coreWidth * 2.2, -coreWidth * 0.2, offset)
            * (1.0 - smoothstep(-coreWidth * 0.2, coreWidth * 0.8, offset));
        float angle = atan(fromImpact.y, fromImpact.x);
        float arcLight = 0.93 + 0.07 * sin(angle * 18.0 - CameraTime.w * 7.0 + frame.y * 31.0);
        energy = max(energy, max(core, trail * 0.62 + leadingGlow * 0.2) * arcLight * frame.x);
    }

    return energy;
}

void main() {
    float rawDepth = texture(DepthSampler, TexCoord).r;
    if (rawDepth <= 0.000001) discard;

    vec3 viewPosition = viewFromDepth(TexCoord, rawDepth);
    vec3 relativeWorld = (InverseView * vec4(viewPosition, 0.0)).xyz;
    vec3 normal = normalize(cross(dFdx(relativeWorld), dFdy(relativeWorld)));
    vec3 towardCamera = normalize(-relativeWorld);
    if (dot(normal, towardCamera) < 0.0) normal = -normal;
    vec3 worldPosition = CameraTime.xyz + relativeWorld;
    if (Color.a > -9000.0 && worldPosition.y >= Color.a - 0.35 && worldPosition.y <= Color.a + 4.35) discard;
    float alpha = clamp(sonarEnergy(relativeWorld), 0.0, 1.0);
    if (alpha <= 0.002) discard;
    fragColor = vec4(alpha);
}
