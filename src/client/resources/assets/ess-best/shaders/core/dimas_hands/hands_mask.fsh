#version 330

uniform sampler2D HandSampler;
uniform sampler2D HandDepthSampler;

in vec2 TexCoord;
out vec4 fragColor;

void main() {
    float alpha = texture(HandSampler, TexCoord).a;
    float depth = texture(HandDepthSampler, TexCoord).r;
    float coverage = max(smoothstep(0.001, 0.08, alpha), step(0.000001, depth));
    fragColor = vec4(coverage);
}
