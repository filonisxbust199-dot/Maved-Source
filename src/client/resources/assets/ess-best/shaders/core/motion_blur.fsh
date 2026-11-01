#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform MotionBlur {
    mat4 InverseProjection;
    mat4 InverseView;
    mat4 PrevProjection;
    mat4 PrevView;
    // xyz: this frame's camera position minus last frame's; w: shutter (blur strength).
    vec4 CameraDelta;
    // x: samples, y: longest blur as a part of the screen height, zw: screen size in pixels.
    vec4 Params;
};
uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;
layout(location=0) in vec2 texCoord;
layout(location=0) out vec4 fragColor;

// Where this pixel was on screen last frame. Depth is reversed (0 is the far plane), and clip z is the raw depth.
vec2 previousUv(vec2 uv) {
    float depth=texture(DepthSampler,uv).r;
    vec4 view=InverseProjection*vec4(uv*2.0-1.0,depth,1.0);
    vec4 clip;
    if(depth<=0.00005) {
        // Sky: a direction at infinity, moved by the turn of the camera only.
        vec3 direction=(InverseView*vec4(view.xyz,0.0)).xyz;
        clip=PrevProjection*(PrevView*vec4(direction,0.0));
    } else {
        // Camera-relative world position now, made relative to last frame's camera.
        vec3 relative=(InverseView*vec4(view.xyz/view.w,1.0)).xyz;
        clip=PrevProjection*(PrevView*vec4(relative+CameraDelta.xyz,1.0));
    }
    if(clip.w<=0.0001) return uv;
    return clip.xy/clip.w*0.5+0.5;
}

void main() {
    vec2 size=Params.zw;
    vec2 pixels=(texCoord-previousUv(texCoord))*size*CameraDelta.w;
    float span=length(pixels), longest=Params.y*size.y;
    if(span>longest) pixels*=longest/span;
    vec3 sharp=texture(SceneSampler,texCoord).rgb;
    if(span<0.5) { fragColor=vec4(sharp,1.0); return; }
    vec2 velocity=pixels/size;
    int samples=int(Params.x);
    // Interleaved gradient noise shifts the taps per pixel, so few samples read as grain, not as copies.
    float noise=fract(52.9829189*fract(dot(gl_FragCoord.xy,vec2(0.06711056,0.00583715))));
    vec3 sum=vec3(0.0);
    for(int i=0;i<64;i++) {
        if(i>=samples) break;
        float t=(float(i)+noise)/float(samples)-0.5;
        sum+=texture(SceneSampler,clamp(texCoord+velocity*t,vec2(0.0),vec2(1.0))).rgb;
    }
    fragColor=vec4(sum/float(samples),1.0);
}
