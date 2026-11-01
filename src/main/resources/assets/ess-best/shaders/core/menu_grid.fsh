#version 330
#extension GL_ARB_separate_shader_objects : require

// Plain menu background: a thin grid fading out from the top centre.
//
// The image never moves, so MenuScene renders it with a sub-pixel jitter and blends the frames
// (blend weight in ModelOffset.x, jitter in ModelOffset.yz) into smooth lines, then stops. It sits
// in the scene target rather than the GUI so that blurred plates have it as their backdrop.

#include <minecraft:dynamictransforms.glsl>

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

const vec3 BACKGROUND = vec3(0.0392);
const float LINE = 0.09;
// Share of the screen height: 34 menu units, 1.75 pixels each at 1080p.
const float CELL = 0.0551;
const float WIDTH = 0.0016;

void main() {
    float aspect = ColorModulator.y;
    vec2 uv = texCoord + ModelOffset.yz;
    vec2 cells = vec2(uv.x * aspect, uv.y) / CELL;
    vec2 line = step(fract(cells), vec2(WIDTH / CELL));
    float grid = max(line.x, line.y);
    float mask = clamp(1.0 - length(vec2((uv.x - 0.5) / 0.8, (1.0 - uv.y) / 0.6)) / 0.75, 0.0, 1.0);
    fragColor = vec4(BACKGROUND + grid * LINE * mask, ModelOffset.x);
}
