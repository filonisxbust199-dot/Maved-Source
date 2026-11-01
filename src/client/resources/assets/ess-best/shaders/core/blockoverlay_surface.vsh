#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};
layout(std140) uniform Projection { mat4 ProjMat; };
layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;
layout(location = 0) out vec2 faceUV;
layout(location = 1) out vec2 faceSize;
layout(location = 2) out vec3 surfacePosition;
layout(location = 3) out float presence;
void main() {
    faceUV = UV0;
    faceSize = Color.rg;
    surfacePosition = Position;
    presence = Color.a;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
