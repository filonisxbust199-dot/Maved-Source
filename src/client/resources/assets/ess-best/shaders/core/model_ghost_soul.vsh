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

out vec2 texCoord0;
out vec4 vertexColor;
out vec3 modelPosition;

void main() {
    vec3 position = Position + ModelOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(position, 1.0);
    texCoord0 = (TextureMat * vec4(UV0, 0.0, 1.0)).xy;
    vertexColor = Color;
    modelPosition = Position;
}
