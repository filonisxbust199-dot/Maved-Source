#version 330
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat; vec4 ColorModulator; vec3 ModelOffset; mat4 TextureMat;
};
uniform sampler2D Sampler0;
in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;
void main() {
    if (texture(Sampler0, texCoord0).a < 0.1) discard;
    fragColor = vec4(vertexColor.rgb * ColorModulator.rgb, 1.0);
}
