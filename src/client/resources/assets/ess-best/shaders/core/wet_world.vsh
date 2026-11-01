#version 150

out vec2 texCoord;

void main() {
    vec2 positions[6] = vec2[](
        vec2(-1.0, -1.0), vec2(1.0, -1.0), vec2(1.0, 1.0),
        vec2(-1.0, -1.0), vec2(1.0, 1.0), vec2(-1.0, 1.0)
    );
    vec2 p = positions[gl_VertexID];
    texCoord = p * 0.5 + 0.5;
    gl_Position = vec4(p, 0.0, 1.0);
}
