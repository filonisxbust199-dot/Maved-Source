#version 330
#extension GL_ARB_separate_shader_objects : require

// ShaderESP, the final pass. In: the entity silhouettes in their colours (the vanilla outline buffer). Glow: the
// same picture blurred. Outside a silhouette the blurred field is a ready soft halo; inside, it is the distance to
// the edge (about 1 deep inside, about 0.5 at the rim), so one value drives both the glow and the fill, and the fill
// sticks to the entity instead of swimming across the screen.

layout(std140) uniform EssEsp {
    // OutlineWidth, GlowStrength, FillOpacity, Pulse
    vec4 Look;
    // Radius
    vec4 Shape;
};

uniform sampler2D InSampler;
uniform sampler2D GlowSampler;

layout(location = 0) in vec2 texCoord;
layout(location = 1) in vec2 oneTexel;

layout(location = 0) out vec4 fragColor;

const float TAU = 6.28318530718;
const int OUTLINE_DIRS = 12;

float coverage(vec4 color) {
    return clamp(max(color.a, max(color.r, max(color.g, color.b))), 0.0, 1.0);
}

void main() {
    float outlineWidth = Look.x, glowStrength = Look.y, fillOpacity = Look.z, pulse = Look.w;
    vec4 source = texture(InSampler, texCoord);
    float mask = coverage(source);
    vec4 blurred = texture(GlowSampler, texCoord);
    float field = clamp(blurred.a, 0.0, 1.0);

    // Nothing here and nothing glowing nearby: most of the frame.
    if (mask <= 0.001 && field <= 0.001) {
        fragColor = vec4(0.0);
        return;
    }

    // The colour comes from the picture itself: inside the silhouette its own, outside the blurred neighbours'.
    vec3 tint = mask > 0.001 ? source.rgb : clamp(blurred.rgb / max(field, 0.001), 0.0, 1.0);

    // The outline band just outside the silhouette: look around at the outline width, and if the silhouette is
    // there but not here, we are right past the edge. Outwards, so thin targets like items keep their body.
    float band = 0.0;
    vec3 bandTint = tint;
    if (outlineWidth > 0.01) {
        for (int d = 0; d < OUTLINE_DIRS; d++) {
            float angle = TAU * (float(d) / float(OUTLINE_DIRS));
            vec4 nearby = texture(InSampler, texCoord + vec2(cos(angle), sin(angle)) * oneTexel * outlineWidth);
            float edge = coverage(nearby) - mask;
            if (edge > band) {
                band = edge;
                bandTint = nearby.rgb;
            }
        }
    }
    float outlineAlpha = clamp(band, 0.0, 1.0);

    // The power pulls the halo in towards the body; without it the glow spreads as a flat, dirty blot.
    float halo = pow(clamp(field - mask, 0.0, 1.0), 1.35) * glowStrength * pulse;
    float glowAlpha = clamp(halo, 0.0, 1.0);

    // Inside, the field falls towards the edge: 1 at the rim, 0 in the middle.
    float rim = clamp((1.0 - field) * 2.0, 0.0, 1.0);
    float fillAlpha = mask > 0.001 ? clamp(fillOpacity * (0.25 + 0.75 * smoothstep(0.05, 0.95, rim)), 0.0, 1.0) : 0.0;

    // The outline core leans to white, so the edge reads on any background while the colour stays in the halo.
    vec3 outlineRgb = mix(bandTint, vec3(1.0), 0.35);

    // Layers composed premultiplied and divided at the end.
    float glowOver = glowAlpha * (1.0 - fillAlpha);
    float alpha = clamp(fillAlpha + glowOver + outlineAlpha, 0.0, 1.0);
    vec3 rgb = tint * fillAlpha + tint * glowOver + outlineRgb * outlineAlpha;
    fragColor = vec4(clamp(rgb / max(alpha, 0.001), 0.0, 1.0), alpha);
}
