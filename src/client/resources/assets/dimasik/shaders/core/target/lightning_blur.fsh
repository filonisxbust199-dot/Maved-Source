#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform LightningBlur {
    vec4 Params;
};

uniform sampler2D Sampler0;

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

const int MAX_TAPS = 48;

void main() {
    float blurOffset = Params.x;
    vec2 pixel = 1.0 / vec2(textureSize(Sampler0, 0));
    vec2 direction = blurOffset >= 0.0
        ? vec2(pixel.x, 0.0)
        : vec2(0.0, pixel.y);

    float sigma = max(abs(blurOffset) * 1.5, 0.5);
    int radius = min(MAX_TAPS, int(ceil(sigma * 2.0)));
    float twoSigmaSquared = 2.0 * sigma * sigma;

    vec4 sum = texture(Sampler0, texCoord);
    float weightSum = 1.0;
    for (int i = 1; i <= MAX_TAPS; i += 2) {
        if (i > radius) {
            break;
        }
        float firstWeight = exp(-float(i * i) / twoSigmaSquared);
        float pairWeight = firstWeight;
        float offset = float(i);
        if (i + 1 <= radius) {
            float secondWeight = exp(-float((i + 1) * (i + 1)) / twoSigmaSquared);
            pairWeight = firstWeight + secondWeight;
            offset = (float(i) * firstWeight + float(i + 1) * secondWeight) / pairWeight;
        }
        vec2 stepOffset = direction * offset;
        sum += pairWeight * (texture(Sampler0, texCoord + stepOffset)
                + texture(Sampler0, texCoord - stepOffset));
        weightSum += 2.0 * pairWeight;
    }
    fragColor = sum / weightSum;
}
