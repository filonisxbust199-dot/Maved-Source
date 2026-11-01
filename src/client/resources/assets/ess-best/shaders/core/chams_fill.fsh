#version 330
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat; vec4 ColorModulator; vec3 ModelOffset; mat4 TextureMat;
};
uniform sampler2D Sampler0;
in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;
void main() {
    // Preserve transparent outer-skin/custom-model cutouts, but ignore all skin RGB.
    if (texture(Sampler0, texCoord0).a < 0.1) discard;
    fragColor = vertexColor * ColorModulator;
}
