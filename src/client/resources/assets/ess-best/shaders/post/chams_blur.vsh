#version 150
out vec2 texCoord;
void main() {
    vec2 p[6] = vec2[](vec2(-1,-1),vec2(1,-1),vec2(1,1),vec2(-1,-1),vec2(1,1),vec2(-1,1));
    vec2 v = p[gl_VertexID]; texCoord = v * 0.5 + 0.5; gl_Position = vec4(v,0,1);
}
