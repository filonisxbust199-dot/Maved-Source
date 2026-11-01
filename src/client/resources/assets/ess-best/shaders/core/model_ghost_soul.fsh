#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};

uniform sampler2D Sampler0;
layout(location = 0) in vec2 texCoord0;
layout(location = 1) in vec4 vertexColor;
layout(location = 2) in vec3 modelPosition;
layout(location = 0) out vec4 fragColor;

void main() {
    float coverage = texture(Sampler0, texCoord0).a;
    if (coverage < 0.05) discard;
    float t = ColorModulator.x;
    vec3 worldPosition = modelPosition + ColorModulator.yzw;
    float flow = sin(worldPosition.y * 8.0 - t * 3.4 + worldPosition.x * 2.0) * 0.5 + 0.5;
    float fine = sin(worldPosition.y * 27.0 + t * 4.2) * 0.5 + 0.5;
    vec3 body = mix(vertexColor.rgb * 0.50, vertexColor.rgb + vec3(0.23, 0.30, 0.40), flow * 0.72);
    body += vec3(0.12, 0.18, 0.26) * fine * 0.18;
    fragColor = vec4(clamp(body, 0.0, 1.0), vertexColor.a * coverage * (0.72 + flow * 0.28));
}
