#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(location = 0) in vec3 Position;
// rgb: colour; a: strength.
layout(location = 1) in vec4 Color;
// Path: x = distance along it in blocks, y = -1..1 across the ribbon. Landing: the point on the disk, -1..1.
layout(location = 2) in vec2 UV0;
// x: what is drawn (0 path, 1 landing on a block, 2 hit on an entity, 3 splash radius); y: clock in thousandths.
layout(location = 3) in ivec2 UV1;
layout(location = 4) in ivec2 UV2;
layout(location = 5) in vec3 Normal;

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out vec2 local;
layout(location = 2) flat out int mode;
layout(location = 3) flat out float clock;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    local = UV0;
    mode = UV1.x;
    clock = float(UV1.y) / 1000.0;
}
