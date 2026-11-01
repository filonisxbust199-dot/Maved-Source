#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform ChamsGlow { vec4 Params; };
uniform sampler2D VisibleSampler;
uniform sampler2D CloseSampler;
uniform sampler2D WideSampler;
layout(location=0) in vec2 texCoord;
layout(location=0) out vec4 fragColor;
void main() {
    vec4 body=texture(VisibleSampler,texCoord);
    vec4 closeGlow=texture(CloseSampler,texCoord);
    vec4 wideGlow=texture(WideSampler,texCoord);
    float alpha=body.a*clamp(Params.x,0.0,1.0);
    vec3 light=(closeGlow.rgb*1.25+wideGlow.rgb*1.35)*max(0.0,Params.y);
    float peak=max(light.r,max(light.g,light.b));
    vec3 halo=light/(1.0+peak);
    vec3 emission=mix(halo,light*0.06,body.a);
    vec3 color=body.rgb*alpha+emission;
    float outAlpha=clamp(alpha+max(emission.r,max(emission.g,emission.b)),0.0,1.0);
    fragColor=vec4(color,outAlpha);
}
