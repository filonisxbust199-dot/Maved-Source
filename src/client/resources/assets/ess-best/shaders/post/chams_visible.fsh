#version 150
uniform sampler2D BodySampler;
uniform sampler2D BodyDepthSampler;
uniform sampler2D ArmorDepthSampler;
uniform sampler2D TerrainDepthSampler;
uniform sampler2D SceneDepthSampler;
layout(std140) uniform ChamsVisibility { vec4 Options; };
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec4 body = texture(BodySampler, texCoord);
    if (body.a < 0.001) { fragColor = vec4(0.0); return; }
    float depth = texture(BodyDepthSampler, texCoord).r;
    // Minecraft 26.2 uses reverse depth: larger values are nearer.
    if (Options.x < 0.5) {
        if (depth < texture(TerrainDepthSampler, texCoord).r - 0.000001) {
            fragColor = vec4(0.0); return;
        }
        // Also respect held items and other entities when all occlusion is enabled.
        if (Options.y < 0.5 && depth < texture(SceneDepthSampler, texCoord).r - 0.000001) {
            fragColor = vec4(0.0); return;
        }
    }
    if (Options.y < 0.5 && texture(ArmorDepthSampler, texCoord).r > depth + 0.0000001) {
        fragColor = vec4(0.0); return;
    }
    fragColor = body;
}
