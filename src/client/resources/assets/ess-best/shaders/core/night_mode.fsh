#version 150
uniform sampler2D Sampler0;
layout(std140) uniform NightModeInfo { vec4 Params; };
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec4 src = texture(Sampler0, texCoord);
    float luma = dot(src.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 saturated = mix(vec3(luma), src.rgb, clamp(Params.y, 0.0, 2.0));
    fragColor = vec4(saturated * (1.0 - clamp(Params.x, 0.0, 1.0)), src.a);
}
