#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform ChamsGlow { vec4 Params; };
uniform sampler2D InSampler;
layout(location=0) in vec2 texCoord;
layout(location=0) out vec4 fragColor;
void main() {
    vec4 sum=vec4(0.0);
    float total=0.0;
    int steps=clamp(int(ceil(Params.z)),8,64);
    for(int tap=-steps;tap<=steps;tap++) {
        float t=float(tap)/float(steps);
        float w=exp(-t*t*4.5);
        vec2 uv=texCoord+Params.xy*Params.z*t;
        vec4 sampleColor=any(lessThan(uv,vec2(0.0)))||any(greaterThan(uv,vec2(1.0)))?vec4(0.0):texture(InSampler,uv);
        sum+=sampleColor*w;
        total+=w;
    }
    fragColor=sum/max(total,0.0001);
}
