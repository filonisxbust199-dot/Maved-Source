#version 330
#extension GL_ARB_separate_shader_objects : require

// The ESS UI in one pipeline: rounded panels, their shadows and outlines (signed distance, one quad each), MSDF text
// and line icons. One pipeline and one texture pair for all of them lets the GUI renderer batch a whole HUD layer
// into a single draw instead of switching between shape, text and icon pipelines.

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
// Shapes: position from the shape's centre. Text: atlas uv. Icons: 24x24 icon grid.
layout(location = 2) in vec2 UV0;
// Shapes: half size.
layout(location = 3) in vec2 UV3;
// x: mode (0 fill, 1 shadow, 2 outline, 3 text, 4 icon) + 8 circular corners + 16 second font; y: radius*16 or icon id.
layout(location = 4) in ivec2 UV1;
// x: blur or outline width *16, text px range *16, icon stroke; y: unused.
layout(location = 5) in ivec2 UV2;

layout(location = 0) out vec2 local;
layout(location = 1) out vec2 halfSize;
layout(location = 2) flat out ivec4 params;
layout(location = 3) out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    local = UV0;
    halfSize = UV3;
    params = ivec4(UV1, UV2);
    vertexColor = Color;
}
