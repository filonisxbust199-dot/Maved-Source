#version 150

uniform sampler2D Sampler0; // snapshot of the scene color

layout(std140) uniform GradeSettings {
    // x = saturation, y = gamma, z = contrast, w = strength (0..1 wet/dry mix)
    vec4 GradeParams;
    // x = independent CustomWorld saturation (1 = untouched)
    vec4 WorldParams;
};

in vec2 texCoord;
out vec4 fragColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

void main() {
    vec4 src = texture(Sampler0, texCoord);
    vec3 color = src.rgb;

    float saturation = GradeParams.x;
    float gamma = GradeParams.y;
    float contrast = GradeParams.z;
    float strength = GradeParams.w;

    // gamma: pull the whole image brighter/darker with a perceptual power curve
    color = pow(max(color, vec3(0.0)), vec3(1.0 / max(gamma, 0.001)));

    // contrast around mid grey (0.5)
    color = (color - 0.5) * contrast + 0.5;

    // saturation: blend toward the luminance grey
    float lum = dot(clamp(color, 0.0, 1.0), LUMA);
    color = mix(vec3(lum), color, saturation);

    color = clamp(color, 0.0, 1.0);

    // wet/dry mix so the slider "strength" fades the whole grade in and out
    vec3 graded = mix(src.rgb, color, strength);
    float worldSaturation = WorldParams.x;
    float worldLuma = dot(graded, LUMA);
    graded = clamp(mix(vec3(worldLuma), graded, worldSaturation), 0.0, 1.0);
    fragColor = vec4(graded, src.a);
}
