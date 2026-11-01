#version 330

uniform sampler2D TrailSampler;

layout(std140) uniform TrailDisplace {
    vec2 Offset;
    vec2 TextureSize;
    vec4 Params;
    float Flicker;
};

in vec2 TexCoord;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void main() {
    vec2 pixel = 1.0 / max(TextureSize, vec2(1.0));
    float time = Params.y;
    float delta = Params.z;

    float waveA = sin(TexCoord.y * 23.0 - time * 3.2);
    float waveB = sin(TexCoord.y * 47.0 + TexCoord.x * 13.0 + time * 2.1);
    float grain = hash(floor(TexCoord * TextureSize * 0.12) + floor(time * 12.0));
    float electricFlow = (waveA * 0.65 + waveB * 0.25 + (grain - 0.5) * 0.18) * Params.w;
    vec2 flow = Offset + vec2(electricFlow * pixel.x * 2.2, Offset.y * TexCoord.y);
    vec2 uv = TexCoord - flow;

    vec4 history = texture(TrailSampler, uv) * 0.38;
    history += texture(TrailSampler, uv + vec2(pixel.x * 1.4, 0.0)) * 0.17;
    history += texture(TrailSampler, uv - vec2(pixel.x * 1.4, 0.0)) * 0.17;
    history += texture(TrailSampler, uv + vec2(0.0, pixel.y * 1.2)) * 0.12;
    history += texture(TrailSampler, uv - vec2(0.0, pixel.y * 1.2)) * 0.12;
    history += texture(TrailSampler, uv + vec2(pixel.x, pixel.y)) * 0.02;
    history += texture(TrailSampler, uv - vec2(pixel.x, pixel.y)) * 0.02;

    float frameScale = clamp(delta * 60.0, 0.0, 6.0);
    float retention = pow(clamp(1.0 - Params.x, 0.70, 0.999), frameScale);
    float oldAlpha = history.a;
    float newAlpha = oldAlpha * retention;
    float alphaScale = oldAlpha > 0.0001 ? newAlpha / oldAlpha : 0.0;



    vec2 chroma = vec2(pixel.x * (0.7 + abs(waveA) * 0.8), 0.0);
    float red = texture(TrailSampler, uv + chroma).r;
    float blue = texture(TrailSampler, uv - chroma).b;
    vec3 spectral = mix(history.rgb, vec3(red, history.g, blue), 0.16);

    float pulse = 0.965 + 0.035 * sin(time * 7.0 + TexCoord.y * 31.0);
    fragColor = vec4(spectral * alphaScale * pulse, newAlpha);
}
