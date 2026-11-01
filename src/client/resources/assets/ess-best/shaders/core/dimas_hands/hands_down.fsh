#version 330

layout(std140) uniform FogBlurDown {
    vec2 Offset;
    vec2 HalfPixel;
    int Extract;
};

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec2 TexCoord;

out vec4 fragColor;

void main() {
    vec2 sourceTexel = 1.0 / max(vec2(textureSize(Sampler0, 0)), vec2(1.0));
    vec2 low = sourceTexel * 0.5;
    vec2 high = vec2(1.0) - low;
    vec2 u = clamp(TexCoord, low, high);
    vec2 h = HalfPixel * Offset;
    vec2 p0 = u;
    vec2 p1 = clamp(u - h, low, high);
    vec2 p2 = clamp(u + h, low, high);
    vec2 p3 = clamp(u + vec2(h.x, -h.y), low, high);
    vec2 p4 = clamp(u - vec2(h.x, -h.y), low, high);
    vec4 r = (texture(Sampler0, p0) * 4.0
        + texture(Sampler0, p1)
        + texture(Sampler0, p2)
        + texture(Sampler0, p3)
        + texture(Sampler0, p4)) / 8.0;
    if (Extract == 1) {
        float depthCov = ((step(0.000001, texture(Sampler1, p0).r)) * 4.0
            + (step(0.000001, texture(Sampler1, p1).r))
            + (step(0.000001, texture(Sampler1, p2).r))
            + (step(0.000001, texture(Sampler1, p3).r))
            + (step(0.000001, texture(Sampler1, p4).r))) / 8.0;
        float cov = max(clamp(r.a, 0.0, 1.0), depthCov);
        r = vec4(vec3(cov), cov);
    }
    fragColor = r;
}
