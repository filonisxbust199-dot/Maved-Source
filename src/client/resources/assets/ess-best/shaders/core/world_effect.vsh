#version 150
out vec2 vUv;
void main() {
    vec2 p[3] = vec2[](vec2(-1.0, -1.0), vec2(3.0, -1.0), vec2(-1.0, 3.0));
    gl_Position = vec4(p[gl_VertexID], 0.0, 1.0);
    vUv = p[gl_VertexID] * 0.5 + 0.5;
}
