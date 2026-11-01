#version 330

uniform sampler2D InSampler;
layout(std140) uniform BlurInfo { float Offset; };
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 o = (0.5 * Offset) / vec2(textureSize(InSampler, 0));
    vec3 sum = texture(InSampler, texCoord + vec2(-2.0 * o.x, 0.0)).rgb;
    sum += texture(InSampler, texCoord + vec2(-o.x, o.y)).rgb * 2.0;
    sum += texture(InSampler, texCoord + vec2(0.0, 2.0 * o.y)).rgb;
    sum += texture(InSampler, texCoord + vec2(o.x, o.y)).rgb * 2.0;
    sum += texture(InSampler, texCoord + vec2(2.0 * o.x, 0.0)).rgb;
    sum += texture(InSampler, texCoord + vec2(o.x, -o.y)).rgb * 2.0;
    sum += texture(InSampler, texCoord + vec2(0.0, -2.0 * o.y)).rgb;
    sum += texture(InSampler, texCoord + vec2(-o.x, -o.y)).rgb * 2.0;
    fragColor = vec4(sum / 12.0, 1.0);
}
