#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(location = 0) in vec3 Position;
// rgb: tint; a: glow strength.
layout(location = 1) in vec4 Color;
// The point in the figure's own frame, -1..1 on both axes.
layout(location = 2) in vec2 UV0;
// x: progress in thousandths; y: wobble strength (hundreds) and seed (units).
layout(location = 3) in ivec2 UV1;
// x: clock in thousandths of a loop; y: shape id, plus 16 for a landing.
layout(location = 4) in ivec2 UV2;
layout(location = 5) in vec3 Normal;

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out vec2 local;
layout(location = 2) flat out vec4 params;
layout(location = 3) flat out int kind;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    local = UV0;
    params = vec4(float(UV1.x) / 1000.0, float(UV1.y / 100) / 100.0, float(UV1.y - UV1.y / 100 * 100), float(UV2.x) / 1000.0);
    kind = UV2.y;
}
