#version auto

in vec4 position;

// Blur renders in texture space, independent of HUD scale and animated GUI matrices.
void main() {
    gl_Position = vec4(position.xy, 0.0, 1.0);
}
