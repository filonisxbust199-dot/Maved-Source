#version 330

uniform sampler2D InSampler;
layout(std140) uniform BlurInfo { float Offset; };
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 o = Offset / vec2(textureSize(InSampler, 0));
    vec3 sum = texture(InSampler, texCoord).rgb * 4.0;
    sum += texture(InSampler, texCoord - o).rgb;
    sum += texture(InSampler, texCoord + o).rgb;
    sum += texture(InSampler, texCoord + vec2(o.x, -o.y)).rgb;
    sum += texture(InSampler, texCoord + vec2(-o.x, o.y)).rgb;
    fragColor = vec4(sum * 0.125, 1.0);
}
