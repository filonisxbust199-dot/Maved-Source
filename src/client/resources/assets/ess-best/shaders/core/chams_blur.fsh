#version 150
uniform sampler2D SceneSampler;
uniform sampler2D MaskSampler;
uniform sampler2D SceneDepthSampler;
uniform sampler2D MaskDepthSampler;
layout(std140) uniform ChamsBlur { vec4 Tint; vec4 Params; };
in vec2 texCoord;
out vec4 fragColor;
const int TAPS = 16;
const float GOLDEN = 2.39996323;
void main() {
    vec4 maskSample = texture(MaskSampler, texCoord);
    float mask = maskSample.a;
    if (mask <= 0.001) discard;
    if (Params.z < 0.5) {
        float sceneDepth = texture(SceneDepthSampler, texCoord).r;
        float modelDepth = texture(MaskDepthSampler, texCoord).r;
        if (modelDepth < sceneDepth - 0.000001) discard;
    }
    vec2 texel = 1.0 / vec2(textureSize(SceneSampler, 0));
    vec2 uv = Params.y > 0.5 ? vec2(1.0 - texCoord.x, texCoord.y) : texCoord;
    vec3 blurred = vec3(0.0); float weight = 0.0;
    for (int i = 0; i < TAPS; i++) {
        float t = (float(i) + 0.5) / float(TAPS);
        float w = exp(-t * 1.8);
        float d = sqrt(t) * max(0.5, Params.x);
        vec2 off = vec2(cos(float(i)*GOLDEN), sin(float(i)*GOLDEN)) * texel * d;
        blurred += texture(SceneSampler, clamp(uv + off, vec2(0), vec2(1))).rgb * w;
        weight += w;
    }
    blurred /= max(weight, 0.0001);
    float lum = dot(blurred, vec3(0.2126,0.7152,0.0722));
    vec3 colored = maskSample.rgb * (0.32 + lum * 0.92) + blurred * maskSample.rgb * 0.16;
    vec3 glass = mix(blurred, colored, clamp(Tint.a,0.0,1.0));
    vec3 original = texture(SceneSampler, texCoord).rgb;
    fragColor = vec4(mix(original, glass, smoothstep(0.0,0.82,mask)), 1.0);
}
