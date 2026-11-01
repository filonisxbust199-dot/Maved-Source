#version 330

uniform sampler2D TrailSampler;

layout(std140) uniform TrailDisplace {
    vec2 Offset;
    vec2 TextureSize;
    vec4 Params;
    vec4 MotionParams;
};

in vec2 TexCoord;
out vec4 fragColor;

vec4 historyAt(vec2 uv) {
    if (any(lessThan(uv, vec2(0.0))) || any(greaterThan(uv, vec2(1.0)))) return vec4(0.0);
    return texture(TrailSampler, uv);
}

void main() {
    vec2 pixel = 1.0 / max(TextureSize, vec2(1.0));
    vec2 position = TexCoord * vec2(TextureSize.x / max(TextureSize.y, 1.0), 1.0);
    float delta = clamp(Params.z, 0.0, 0.1);
    float time = Params.y - delta * MotionParams.x * 0.5;
    vec2 flow = vec2(
            cos(position.y * 13.0 - time * 1.7) * sin(position.x * 9.0 + time * 1.1),
            -sin(position.y * 13.0 - time * 1.7) * cos(position.x * 9.0 + time * 1.1) * 0.45);
    flow *= Params.w * delta * 0.10;
    flow.x /= max(TextureSize.x / max(TextureSize.y, 1.0), 0.1);
    vec2 uv = TexCoord - Offset - flow;
    float diffusion = min(delta * 60.0 * 0.06, 0.3);
    vec4 history = historyAt(uv) * (1.0 - diffusion * 2.0);
    history += historyAt(uv + pixel * vec2(0.7, 0.0)) * diffusion;
    history += historyAt(uv - pixel * vec2(0.7, 0.0)) * diffusion;
    float retention = pow(clamp(1.0 - Params.x, 0.0, 1.0), delta * 60.0);
    vec4 result = history * retention;
    uint seed = uint(gl_FragCoord.x) * 1973u ^ uint(gl_FragCoord.y) * 9277u ^ uint(MotionParams.y) * 26699u;
    seed ^= seed >> 16;
    seed *= 0x7feb352du;
    seed ^= seed >> 15;
    float threshold = float(seed & 65535u) / 65536.0;
    float alpha = floor(result.a * 255.0 + threshold) / 255.0;
    fragColor = result.a > 0.0 ? vec4(result.rgb * (alpha / result.a), alpha) : vec4(0.0);
}
