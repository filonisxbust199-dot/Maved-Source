#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(location = 0) in vec3 Position;
// rgb: tint; a: strength of this vertex (fades along trails).
layout(location = 1) in vec4 Color;
// Lines: x = along, y = -1..1 across. Sprites: the point on the sprite, -1..1. Sheets: y = 0 at the feet, 1 at the top.
layout(location = 2) in vec2 UV0;
// x: what is drawn (see glow_fx.fsh); y: clock in thousandths of a loop.
layout(location = 3) in ivec2 UV1;
// x: glow in thousandths; y: a parameter in thousandths (line core width, sprite progress, fill strength).
layout(location = 4) in ivec2 UV2;
layout(location = 5) in vec3 Normal;

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out vec2 local;
layout(location = 2) flat out ivec2 kind;
layout(location = 3) flat out vec2 params;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    local = UV0;
    kind = ivec2(UV1.x, UV1.y);
    params = vec2(float(UV2.x) / 1000.0, float(UV2.y) / 1000.0);
}
