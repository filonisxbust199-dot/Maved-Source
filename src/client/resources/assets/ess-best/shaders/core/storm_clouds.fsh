#version 150
#moj_import <ess-best:storm_clouds.glsl>
in vec2 vUv;
out vec4 fragColor;
void main() {
    fragColor = vec4(0);
    if (Weather.x <= .001 || Weather.y <= .001) return;
    vec3 viewRay = normalize(viewPosition(vUv, .9999));
    vec3 ray = normalize(mat3(InvView) * viewRay);
    float start, end;
    if (abs(ray.y) < .0001) {
        if (CameraTime.y < Layer.x || CameraTime.y > Layer.x + Layer.y) return;
        start = 0.0; end = Layer.w;
    } else {
        float a = (Layer.x - CameraTime.y) / ray.y;
        float b = (Layer.x + Layer.y - CameraTime.y) / ray.y;
        start = max(0.0, min(a, b)); end = min(Layer.w, max(a, b));
    }
    end = min(end, sceneDistance(vUv) - .1);
    if (end <= start || start >= Layer.w) return;
    float stride = (end - start) / SunSteps.w;
    float jitter = .35 + hash(floor(vUv * Resolution.zw)) * .3;
    float travel = start + stride * jitter;
    vec3 sum = vec3(0); float transmittance = 1.0;
    vec3 sun = normalize(SunSteps.xyz);
    float day = .24 + max(sun.y, 0.0) * .76;
    float forwardLight = pow(max(dot(ray, sun), 0.0), 12.0) * ShadowColor.w;
    for (int i = 0; i < 56; ++i) {
        if (i >= int(SunSteps.w) || travel >= end || transmittance < .015) break;
        vec3 p = CameraTime.xyz + ray * travel;
        float density = cloudDensity(p);
        if (density > .001) {
            float alpha = 1.0 - exp(-density * stride * .075);
            // One sunward probe darkens the interior without a second ray march.
            float ahead = cloudDensity(p + sun * Layer.y * .28);
            float direct = exp(-ahead * 3.4);
            float heightLight = smoothstep(.08, .95, (p.y - Layer.x) / Layer.y);
            float lighting = clamp(.20 + heightLight * .18 + direct * .62, 0.0, 1.0);
            vec3 cloudLight = mix(ShadowColor.rgb, LightColor.rgb, lighting) * day * LightColor.w;
            float rim = sqrt(max(density - ahead, 0.0));
            cloudLight += LightColor.rgb * forwardLight * direct * rim * .9;
            sum += cloudLight * alpha * transmittance;
            transmittance *= 1.0 - alpha;
        }
        travel += stride;
    }
    float distanceFade = 1.0 - smoothstep(Layer.w * .68, Layer.w, start);
    fragColor = vec4(clamp(sum, 0.0, 1.0) * distanceFade, (1.0 - transmittance) * distanceFade);
}
