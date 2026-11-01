#version 330
#extension GL_ARB_separate_shader_objects : require

// ShaderESP: a Gaussian along one axis; the chain runs it along X, then Y. The step stretches with the radius, so
// the cost does not depend on it, and bilinear sampling fills the gaps between taps.

layout(std140) uniform EssEsp {
    vec4 Look;
    vec4 Shape;
};

uniform sampler2D InSampler;

layout(location = 0) in vec2 texCoord;
layout(location = 1) in vec2 sampleStep;

layout(location = 0) out vec4 fragColor;

const int TAPS = 8;

void main() {
    float radius = Shape.x;
    if (radius <= 0.01) {
        fragColor = texture(InSampler, texCoord);
        return;
    }
    float step = radius / float(TAPS);
    // The kernel fades out right at the radius: at three sigma the weight is about 0.011.
    float sigma = radius / 3.0;
    float twoSigmaSq = 2.0 * sigma * sigma;
    vec4 sum = texture(InSampler, texCoord);
    float total = 1.0;
    for (int i = 1; i <= TAPS; i++) {
        float offset = float(i) * step;
        float weight = exp(-offset * offset / twoSigmaSq);
        sum += texture(InSampler, texCoord + sampleStep * offset) * weight;
        sum += texture(InSampler, texCoord - sampleStep * offset) * weight;
        total += weight * 2.0;
    }
    fragColor = sum / total;
}
