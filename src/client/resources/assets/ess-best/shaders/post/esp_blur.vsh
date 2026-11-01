#version 330
#extension GL_ARB_separate_shader_objects : require

// Full-screen triangle with the sampling step of a one-axis blur (the axis comes from the pass).

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};
layout(std140) uniform EssBlur {
    vec2 BlurDir;
};

layout(location = 0) out vec2 texCoord;
layout(location = 1) out vec2 sampleStep;

void main() {
    vec2 uv = vec2((gl_VertexIndex << 1) & 2, gl_VertexIndex & 2);
    gl_Position = vec4(uv * 2.0 - 1.0, 0.0, 1.0);
    texCoord = uv;
    sampleStep = BlurDir / InSize;
}
