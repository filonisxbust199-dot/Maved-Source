#version 150

// exosware "Hit Vortex" — the procedural branch ported verbatim: two interleaved
// sinusoidal swirls in polar space, a radial envelope ring, a faint halo, and a
// white-hot boost on the brightest streaks. Premultiplied output with a quarter
// alpha so the light adds over the scene instead of flattening it.
in vec2 localUV;
flat in float vAng;
flat in vec4 vColor;

out vec4 fragColor;

void main() {
    vec2 p = localUV * 2.0 - 1.0;
    float r = length(p);
    if (r >= 1.0) {
        discard;
    }

    float theta = atan(p.y, p.x);
    float sw1 = theta * 2.0 + r * 7.0 - vAng;
    float sw2 = theta * 3.0 + r * 9.0 + 1.7 - vAng * 1.2;
    float s1 = 0.5 + 0.5 * sin(sw1);
    float s2 = 0.5 + 0.5 * sin(sw2);
    float env = smoothstep(0.28, 0.5, r) * (1.0 - smoothstep(0.78, 1.0, r));
    float halo = smoothstep(0.2, 0.45, r) * (1.0 - smoothstep(0.6, 1.0, r)) * 0.12;
    float intensity = env * (pow(s1, 20.0) * 1.3 + pow(s2, 14.0) * 0.7 + pow(s1, 2.5) * 0.22) + halo;
    intensity = min(intensity, 1.0);

    float a = intensity * vColor.a;
    vec3 rgb = vColor.rgb * a + vec3(0.4) * pow(intensity, 3.0) * vColor.a;
    rgb = min(rgb, vec3(1.0));
    fragColor = vec4(rgb, a * 0.25);
}
