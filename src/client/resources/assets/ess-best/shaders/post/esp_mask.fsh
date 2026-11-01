#version 330
#extension GL_ARB_separate_shader_objects : require

// ShaderESP, pass 1: the entity outline buffer turned into a "silhouette and its colour" field. Coverage is the
// largest channel, since vanilla fills different channels in different cases. The colour is premultiplied by the
// coverage, so the same blur passes can soften it and the last pass gets it back by dividing by alpha: every
// target glows in its own colour.

uniform sampler2D InSampler;

layout(location = 0) in vec2 texCoord;
layout(location = 1) in vec2 oneTexel;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 source = texture(InSampler, texCoord);
    float mask = clamp(max(source.a, max(source.r, max(source.g, source.b))), 0.0, 1.0);
    fragColor = vec4(source.rgb * mask, mask);
}
