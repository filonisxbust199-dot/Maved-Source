#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>

// Seasons weather: drawn on the vanilla weather columns with the season textures. Soft alpha
// instead of a hard cutoff and a gentle glow on snow.
//
// The columns are vertical walls standing around the camera; looking up you see all of them
// converge into one point, which reads as a tunnel of streaks. Precipitation that is steeply above
// the camera, or right next to it, is therefore faded out: from below you only see it falling
// in the distance, as in real snowfall.

uniform sampler2D Sampler0;

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
layout(location = 2) in vec2 texCoord0;
layout(location = 3) in vec4 vertexColor;
layout(location = 4) in vec3 relativePos;

layout(location = 0) out vec4 fragColor;

void main() {
    vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));
    vec4 centre = texture(Sampler0, texCoord0);
    // Four-tap halo: flakes pick up a soft bloom, rain streaks a slight width.
    float halo = (texture(Sampler0, texCoord0 + vec2(texel.x * 1.5, 0.0)).a
                + texture(Sampler0, texCoord0 - vec2(texel.x * 1.5, 0.0)).a
                + texture(Sampler0, texCoord0 + vec2(0.0, texel.y * 1.5)).a
                + texture(Sampler0, texCoord0 - vec2(0.0, texel.y * 1.5)).a) * 0.25;
    float alpha = max(centre.a, halo * 0.45);
    vec4 color = vec4(centre.rgb * (1.0 + 0.15 * centre.a), alpha) * vertexColor * ColorModulator;

    float distance = max(sphericalVertexDistance, 1e-3);
    float horizontal = length(relativePos.xz);
    // Elevation of the fragment as seen from the camera: 0 at the horizon, 1 straight up.
    float elevation = relativePos.y / distance;
    float overhead = 1.0 - smoothstep(0.28, 0.62, elevation);
    // Columns hugging the camera are the ones that sweep across the whole view.
    float near = smoothstep(1.5, 4.5, horizontal);
    color.a *= overhead * near;
    if (color.a < 0.01) discard;
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance,
            FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
