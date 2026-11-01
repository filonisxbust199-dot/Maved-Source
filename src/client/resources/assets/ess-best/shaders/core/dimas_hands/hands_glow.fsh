#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec3 glowColor1;
uniform vec3 glowColor2;
uniform float exposure;

in vec2 TexCoord;

out vec4 fragColor;

void main() {
    vec2 u = TexCoord;
    vec4 b = texture(Sampler0, u);
    float depth = texture(Sampler1, u).r;
    float maskA = step(0.0001, depth);
    float bA = b.a * (1.0 - maskA);
    if (bA < 0.002) discard;
    float intensityRaw = bA * exposure;
    float hot = smoothstep(0.65, 1.55, intensityRaw);
    float coreT = pow(clamp(bA, 0.0, 1.0), 0.55);
    vec3 baseCol = mix(glowColor2, glowColor1, coreT);
    vec3 hotCol = vec3(1.0, 0.97, 0.88);
    vec3 col = mix(baseCol, hotCol, hot * 0.85);
    float boost = 1.0 + hot * 0.6;
    float a = clamp(intensityRaw, 0.0, 1.0);
    fragColor = vec4(col * boost, a);
}
