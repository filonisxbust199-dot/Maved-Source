#include<rounded-base>

#inputs {
    vec4 vertexColor;
    vec2 offset;
    vec2 ring;
    vec2 rot;
    vec2 cap;
};

// Arc segment (iq's sdArc). ring = (radius, halfThickness); a NEGATIVE halfThickness selects
// FLAT caps — the sign is a free channel, so both cap styles share one program and batch.
// The CPU pre-rotates nothing — it hands us rot = (cos mid, sin mid) of the arc's middle
// angle and cap = (sin ap, cos ap) of its half-aperture, so the fragment stays trig-free:
// flip to y-up, rotate the arc's middle onto +Y, mirror x, then it's either the distance
// to the ring band (inside the aperture) or to the cap (outside): round cap = distance to
// the endpoint circle, flat cap = distance to the radial end face segment.
void main() {
    vec2 p = vec2(offset.x, -offset.y);
    p = vec2(p.x * rot.x - p.y * rot.y,
             p.x * rot.y + p.y * rot.x);
    p.x = abs(p.x);

    float halfT = abs(ring.y);
    float sd;
    if (cap.y * p.x > cap.x * p.y) {
        if (ring.y < 0.) {
            float t = clamp(dot(p, cap), ring.x - halfT, ring.x + halfT);
            sd = length(p - cap * t);
        } else {
            sd = length(p - cap * ring.x) - halfT;
        }
    } else {
        sd = abs(length(p) - ring.x) - halfT;
    }

    fragColor = vec4(vertexColor.rgb, (1. - smoothstep(0., edgeSoftness, sd)) * vertexColor.a) * shaderColor;
}
