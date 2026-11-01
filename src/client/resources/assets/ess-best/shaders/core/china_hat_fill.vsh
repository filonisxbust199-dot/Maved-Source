#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

layout(std140) uniform Projection {
    mat4 ProjMat;
};

out vec2 hatUv;
out vec4 hatColor;

void main() {
    hatUv = UV0;
    hatColor = Color;
    gl_Position = ProjMat * ModelViewMat * vec4(Position + ModelOffset, 1.0);
}
