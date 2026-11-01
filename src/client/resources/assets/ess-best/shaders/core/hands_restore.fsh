#version 150
uniform sampler2D HandsSampler;
#ifdef MERGE_CAPTURE
uniform sampler2D HandsDepthSampler;
#endif
in vec2 texCoord;
out vec4 fragColor;
void main() {
    fragColor = texture(HandsSampler, texCoord);
#ifdef MERGE_CAPTURE
    float depth = texture(HandsDepthSampler, texCoord).r;
    if (fragColor.a <= 0.0 && depth <= 0.0) discard;
    gl_FragDepth = depth;
#endif
}
