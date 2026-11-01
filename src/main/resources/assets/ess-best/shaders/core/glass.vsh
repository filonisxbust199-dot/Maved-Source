#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec2 UV3;
layout(location = 3) in ivec2 UV1;
layout(location = 4) in vec4 Color;

layout(location = 0) out vec2 localPos;
layout(location = 1) out vec2 halfSize;
layout(location = 2) out vec3 params;
layout(location = 3) out vec4 tint;
layout(location = 4) out vec2 screenUv;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    // Same convention as core/screenquad: texture space follows NDC on every backend.
    screenUv = gl_Position.xy / gl_Position.w * 0.5 + 0.5;
    localPos = UV0;
    halfSize = UV3;
    params = vec3(float(UV1.x) / 16.0, float(UV1.y % 1024) / 10.0, float(UV1.y / 1024) / 31.0);
    tint = Color;
}
