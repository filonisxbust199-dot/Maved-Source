#version 330

out vec2 texCoord0;

void main() {
    const vec2 positions[6] = vec2[](
        vec2(-1.0, -1.0), vec2(1.0, -1.0), vec2(1.0, 1.0),
        vec2(-1.0, -1.0), vec2(1.0, 1.0), vec2(-1.0, 1.0)
    );
    vec2 position = positions[gl_VertexID];
    texCoord0 = position * 0.5 + 0.5;
    gl_Position = vec4(position, 0.0, 1.0);
}
