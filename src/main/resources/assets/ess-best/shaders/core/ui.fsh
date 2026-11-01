#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

layout(location = 0) in vec2 local;
layout(location = 1) in vec2 halfSize;
layout(location = 2) flat in ivec4 params;
layout(location = 3) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

// --- Line icons (from icon.fsh) ---
// Set from params for the progress arc (id 22): UV1.y = progress * 255 * 128 + stroke * 16.
float arcProgress = 0.0;

float seg(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a, ba = b - a;
    return length(pa - ba * clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0));
}
float ring(vec2 p, vec2 c, float r) { return abs(length(p - c) - r); }
float box(vec2 p, vec2 c, vec2 b, float r) {
    vec2 q = abs(p - c) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}
float dot2(vec2 v) { return dot(v, v); }
// Signed distance to a triangle (negative inside).
float tri(vec2 p, vec2 a, vec2 b, vec2 c) {
    vec2 e0 = b - a, e1 = c - b, e2 = a - c;
    vec2 v0 = p - a, v1 = p - b, v2 = p - c;
    vec2 pq0 = v0 - e0 * clamp(dot(v0, e0) / dot(e0, e0), 0.0, 1.0);
    vec2 pq1 = v1 - e1 * clamp(dot(v1, e1) / dot(e1, e1), 0.0, 1.0);
    vec2 pq2 = v2 - e2 * clamp(dot(v2, e2) / dot(e2, e2), 0.0, 1.0);
    float s = sign(e0.x * e2.y - e0.y * e2.x);
    vec2 d = min(min(vec2(dot(pq0, pq0), s * (v0.x * e0.y - v0.y * e0.x)),
                     vec2(dot(pq1, pq1), s * (v1.x * e1.y - v1.y * e1.x))),
                     vec2(dot(pq2, pq2), s * (v2.x * e2.y - v2.y * e2.x)));
    return -sqrt(d.x) * sign(d.y);
}
// Approximate distance to an ellipse outline centred on the origin.
float ellipse(vec2 q, vec2 r) {
    float k = length(q / r);
    return abs(k - 1.0) * k / max(length(q / (r * r)), 1e-4);
}
// Arc of a circle facing +x, spanning +-angle (given as cos, sin), with round ends.
float arc(vec2 p, vec2 c, float r, vec2 cs) {
    vec2 q = p - c;
    if (q.x * cs.y > abs(q.y) * cs.x) return abs(length(q) - r);
    return min(length(q - r * cs), length(q - r * vec2(cs.x, -cs.y)));
}
// Filled quad a-b-c-d from two triangles.
float para(vec2 p, vec2 a, vec2 b, vec2 c, vec2 d) { return min(tri(p, a, b, c), tri(p, a, c, d)); }
// Convex quad (corners in order) as the furthest of its four edge lines: no seam inside, unlike two triangles.
float edgeLine(vec2 p, vec2 a, vec2 b, float o) {
    vec2 e = normalize(b - a);
    return -o * (e.x * (p.y - a.y) - e.y * (p.x - a.x));
}
float quad(vec2 p, vec2 a, vec2 b, vec2 c, vec2 d) {
    vec2 u = b - a, v = c - b;
    float o = sign(u.x * v.y - u.y * v.x);
    return max(max(edgeLine(p, a, b, o), edgeLine(p, b, c, o)), max(edgeLine(p, c, d, o), edgeLine(p, d, a, o)));
}
// Left peak of the Maved M: a leg leaning in and the inner stroke down to the middle point; the right peak mirrors it.
float markHalf(vec2 p) {
    return min(quad(p, vec2(7.0, 3.5), vec2(10.6, 3.5), vec2(6.6, 20.5), vec2(3.0, 20.5)),
               quad(p, vec2(7.0, 3.5), vec2(10.6, 3.5), vec2(13.4, 14.8), vec2(12.0, 19.0)));
}
float heart(vec2 p) {
    vec2 q = vec2(abs(p.x - 12.0), 21.0 - p.y) / 18.0;
    float d;
    if (q.y + q.x > 1.0) d = sqrt(dot2(q - vec2(0.25, 0.75))) - sqrt(2.0) / 4.0;
    else d = sqrt(min(dot2(q - vec2(0.0, 1.0)), dot2(q - 0.5 * max(q.x + q.y, 0.0)))) * sign(q.x - q.y);
    return d * 18.0;
}
float lens(vec2 p) {
    return max(length(p - vec2(12.0, 15.64)) - 10.64, length(p - vec2(12.0, 8.36)) - 10.64);
}
float shield(vec2 p) {
    vec2 q = vec2(abs(p.x - 12.0), p.y);
    float d = seg(q, vec2(0.0, 2.2), vec2(8.0, 5.0));
    d = min(d, seg(q, vec2(8.0, 5.0), vec2(8.0, 12.0)));
    d = min(d, seg(q, vec2(8.0, 12.0), vec2(6.6, 16.4)));
    d = min(d, seg(q, vec2(6.6, 16.4), vec2(3.6, 19.8)));
    return min(d, seg(q, vec2(3.6, 19.8), vec2(0.0, 21.8)));
}
bool shieldInside(vec2 p) {
    float x = abs(p.x - 12.0), y = p.y, hw;
    if (y < 2.2 || y > 21.8) return false;
    if (y < 5.0) hw = (y - 2.2) / 2.8 * 8.0;
    else if (y < 12.0) hw = 8.0;
    else if (y < 16.4) hw = mix(8.0, 6.6, (y - 12.0) / 4.4);
    else if (y < 19.8) hw = mix(6.6, 3.6, (y - 16.4) / 3.4);
    else hw = mix(3.6, 0.0, (y - 19.8) / 2.0);
    return x < hw;
}

// d = distance to the stroke centerline; filled shapes return signed distance and set fill.
float icon(int id, vec2 p, out bool fill) {
    fill = false;
    if (id == 0) return min(seg(p, vec2(20.0, 6.0), vec2(9.0, 17.0)), seg(p, vec2(9.0, 17.0), vec2(4.0, 12.0)));
    if (id == 1) return min(seg(p, vec2(6.0, 9.0), vec2(12.0, 15.0)), seg(p, vec2(12.0, 15.0), vec2(18.0, 9.0)));
    if (id == 2) return min(seg(p, vec2(9.0, 6.0), vec2(15.0, 12.0)), seg(p, vec2(15.0, 12.0), vec2(9.0, 18.0)));
    if (id == 3) { fill = true; return heart(p); }
    if (id == 4) return abs(heart(p));
    if (id == 5) return shield(p);
    if (id == 6) { // sword
        float d = seg(p, vec2(3.0, 3.0), vec2(14.5, 14.5));
        d = min(d, seg(p, vec2(3.0, 3.0), vec2(3.0, 6.0)));
        d = min(d, seg(p, vec2(3.0, 3.0), vec2(6.0, 3.0)));
        d = min(d, seg(p, vec2(13.0, 19.0), vec2(19.0, 13.0)));
        d = min(d, seg(p, vec2(16.0, 16.0), vec2(20.0, 20.0)));
        return min(d, seg(p, vec2(19.0, 21.0), vec2(21.0, 19.0)));
    }
    if (id == 7) { // clock
        float d = ring(p, vec2(12.0), 10.0);
        return min(d, min(seg(p, vec2(12.0, 6.0), vec2(12.0, 12.0)), seg(p, vec2(12.0, 12.0), vec2(16.0, 14.0))));
    }
    if (id == 8) { // map pin
        vec2 c = vec2(12.0, 10.0);
        float outer = p.y < 13.6 ? ring(p, c, 8.0) : 1e3;
        outer = min(outer, seg(vec2(abs(p.x - 12.0), p.y), vec2(7.0, 13.6), vec2(0.0, 22.0)));
        return min(outer, ring(p, c, 3.0));
    }
    if (id == 9) { // user
        float d = ring(p, vec2(12.0, 7.0), 4.0);
        float body = abs(box(p, vec2(12.0, 20.0), vec2(7.0, 5.0), 4.0));
        return min(d, p.y > 21.0 ? 1e3 : body);
    }
    if (id == 10) { // bolt
        float d = seg(p, vec2(13.0, 2.0), vec2(3.0, 14.0));
        d = min(d, seg(p, vec2(3.0, 14.0), vec2(12.0, 14.0)));
        d = min(d, seg(p, vec2(12.0, 14.0), vec2(11.0, 22.0)));
        d = min(d, seg(p, vec2(11.0, 22.0), vec2(21.0, 10.0)));
        d = min(d, seg(p, vec2(21.0, 10.0), vec2(12.0, 10.0)));
        return min(d, seg(p, vec2(12.0, 10.0), vec2(13.0, 2.0)));
    }
    if (id == 11) { // signal bars
        float d = seg(p, vec2(2.0, 20.0), vec2(2.0, 20.0));
        d = min(d, seg(p, vec2(7.0, 20.0), vec2(7.0, 16.0)));
        d = min(d, seg(p, vec2(12.0, 20.0), vec2(12.0, 12.0)));
        d = min(d, seg(p, vec2(17.0, 20.0), vec2(17.0, 8.0)));
        return min(d, seg(p, vec2(22.0, 20.0), vec2(22.0, 4.0)));
    }
    if (id == 12) { // compass
        float d = ring(p, vec2(12.0), 10.0);
        d = min(d, seg(p, vec2(16.24, 7.76), vec2(14.12, 14.12)));
        d = min(d, seg(p, vec2(14.12, 14.12), vec2(7.76, 16.24)));
        d = min(d, seg(p, vec2(7.76, 16.24), vec2(9.88, 9.88)));
        return min(d, seg(p, vec2(9.88, 9.88), vec2(16.24, 7.76)));
    }
    if (id == 13) { // flask
        vec2 q = vec2(abs(p.x - 12.0), p.y);
        float d = seg(q, vec2(2.0, 2.0), vec2(2.0, 9.5));
        d = min(d, seg(q, vec2(2.0, 9.5), vec2(7.6, 20.4)));
        d = min(d, seg(q, vec2(7.6, 20.4), vec2(6.6, 21.6)));
        d = min(d, seg(q, vec2(6.6, 21.6), vec2(0.0, 21.6)));
        d = min(d, seg(q, vec2(0.0, 2.0), vec2(3.5, 2.0)));
        return min(d, seg(q, vec2(0.0, 16.0), vec2(5.0, 16.0)));
    }
    if (id == 14) return min(abs(lens(p)), ring(p, vec2(12.0), 3.0)); // eye
    if (id == 15) { // grid
        float d = abs(box(p, vec2(6.5, 6.5), vec2(3.5), 1.0));
        d = min(d, abs(box(p, vec2(17.5, 6.5), vec2(3.5), 1.0)));
        d = min(d, abs(box(p, vec2(6.5, 17.5), vec2(3.5), 1.0)));
        return min(d, abs(box(p, vec2(17.5, 17.5), vec2(3.5), 1.0)));
    }
    if (id == 16) { // move
        vec2 q = abs(p - 12.0);
        if (q.y > q.x) q = q.yx;
        float d = seg(q, vec2(0.0), vec2(10.0, 0.0));
        return min(d, seg(q, vec2(10.0, 0.0), vec2(7.0, 3.0)));
    }
    if (id == 17) return min(seg(p, vec2(6.0, 6.0), vec2(18.0, 18.0)), seg(p, vec2(18.0, 6.0), vec2(6.0, 18.0)));
    if (id == 18) { // crosshair
        float d = ring(p, vec2(12.0), 8.0);
        d = min(d, seg(p, vec2(12.0, 1.5), vec2(12.0, 6.0)));
        d = min(d, seg(p, vec2(12.0, 18.0), vec2(12.0, 22.5)));
        d = min(d, seg(p, vec2(1.5, 12.0), vec2(6.0, 12.0)));
        return min(d, seg(p, vec2(18.0, 12.0), vec2(22.5, 12.0)));
    }
    if (id == 19) { fill = true; return shield(p) * (shieldInside(p) ? -1.0 : 1.0); }
    if (id == 20) return ring(p, vec2(12.0), 9.5);
    if (id == 21) { fill = true; return length(p - vec2(12.0)) - 10.5; }
    if (id == 22) { // progress arc from 12 o'clock, clockwise, round caps
        vec2 q = p - vec2(12.0);
        float r = 9.0, angle = atan(q.x, -q.y) / 6.2831853;
        if (angle < 0.0) angle += 1.0;
        if (arcProgress >= 0.999) return abs(length(q) - r);
        if (angle <= arcProgress) return abs(length(q) - r);
        float endAngle = arcProgress * 6.2831853;
        vec2 start = vec2(0.0, -r), end = vec2(sin(endAngle), -cos(endAngle)) * r;
        return min(length(q - start), length(q - end));
    }
    if (id == 23) { // keyboard: body, two rows of keys, space bar
        float d = abs(box(p, vec2(12.0), vec2(10.0, 7.0), 2.0));
        for (int i = 0; i < 4; i++) {
            float x = 6.0 + float(i) * 4.0;
            d = min(d, min(length(p - vec2(x, 9.0)), length(p - vec2(x, 12.5))));
        }
        return min(d, seg(p, vec2(8.0, 16.0), vec2(16.0, 16.0)));
    }
    if (id == 24) { // palette: ring with paint dots
        float d = ring(p, vec2(12.0), 10.0);
        d = min(d, length(p - vec2(13.5, 6.5)));
        d = min(d, length(p - vec2(17.5, 10.5)));
        d = min(d, length(p - vec2(8.5, 7.5)));
        return min(d, length(p - vec2(6.5, 12.5)));
    }
    if (id == 25) return min(seg(p, vec2(15.0, 6.0), vec2(9.0, 12.0)), seg(p, vec2(9.0, 12.0), vec2(15.0, 18.0)));
    if (id == 27) { // five-pointed star outline
        float d = 1e3;
        for (int i = 0; i < 10; i++) {
            float a0 = -1.5707963 + float(i) * 0.6283185, a1 = a0 + 0.6283185;
            float r0 = (i % 2 == 0) ? 10.0 : 4.2, r1 = (i % 2 == 0) ? 4.2 : 10.0;
            d = min(d, seg(p, vec2(12.0, 12.8) + r0 * vec2(cos(a0), sin(a0)), vec2(12.0, 12.8) + r1 * vec2(cos(a1), sin(a1))));
        }
        return d;
    }
    if (id == 26) return min(ring(p, vec2(11.0), 7.0), seg(p, vec2(16.2, 16.2), vec2(21.0, 21.0))); // search
    // Media controls, filled: play triangle, pause bars, next / previous (triangle and bar, mirrored).
    if (id == 28) { fill = true; return tri(p, vec2(7.5, 4.5), vec2(7.5, 19.5), vec2(20.0, 12.0)) - 1.2; }
    if (id == 29) { fill = true; return min(box(p, vec2(8.5, 12.0), vec2(2.6, 7.6), 1.4), box(p, vec2(15.5, 12.0), vec2(2.6, 7.6), 1.4)); }
    if (id == 30 || id == 31) {
        fill = true;
        vec2 q = id == 31 ? vec2(24.0 - p.x, p.y) : p;
        return min(tri(q, vec2(5.0, 5.5), vec2(5.0, 18.5), vec2(15.5, 12.0)) - 1.0, box(q, vec2(18.5, 12.0), vec2(1.6, 6.8), 1.0));
    }
    if (id == 32 || id == 33) { // sun; sunset is its upper half over the horizon
        bool set = id == 33;
        vec2 c = set ? vec2(12.0, 17.0) : vec2(12.0);
        float d = ring(p, c, set ? 5.0 : 4.5);
        if (set && p.y > 17.0) d = 1e3;
        for (int i = 0; i < 8; i++) {
            float a = float(i) * 0.7853982;
            vec2 dir = vec2(cos(a), sin(a));
            if (set && dir.y > 0.01) continue;
            d = min(d, seg(p, c + dir * (set ? 8.0 : 7.5), c + dir * (set ? 10.0 : 9.8)));
        }
        if (set) d = min(d, seg(p, vec2(2.0, 20.5), vec2(22.0, 20.5)));
        return d;
    }
    if (id == 34) { fill = true; return max(length(p - vec2(12.0)) - 9.0, -(length(p - vec2(16.5, 8.0)) - 7.2)); } // moon
    // Armor pieces, mirrored around x = 12 (boots: one boot).
    vec2 m = vec2(abs(p.x - 12.0), p.y);
    if (id == 35) { // helmet: dome, cheek guards, face opening
        float d = p.y < 11.0 ? ring(p, vec2(12.0, 11.0), 8.5) : 1e3;
        d = min(d, seg(m, vec2(8.5, 11.0), vec2(8.5, 19.5)));
        d = min(d, seg(m, vec2(8.5, 19.5), vec2(4.5, 19.5)));
        d = min(d, seg(m, vec2(4.5, 19.5), vec2(4.5, 14.0)));
        return min(d, seg(m, vec2(4.5, 14.0), vec2(0.0, 14.0)));
    }
    if (id == 36) { // chestplate: neckline, shoulders, armholes, body
        float d = seg(m, vec2(0.0, 6.5), vec2(3.5, 3.0));
        d = min(d, seg(m, vec2(3.5, 3.0), vec2(7.5, 3.0)));
        d = min(d, seg(m, vec2(7.5, 3.0), vec2(10.5, 6.0)));
        d = min(d, seg(m, vec2(10.5, 6.0), vec2(10.5, 10.0)));
        d = min(d, seg(m, vec2(10.5, 10.0), vec2(7.0, 10.0)));
        d = min(d, seg(m, vec2(7.0, 10.0), vec2(7.0, 21.0)));
        return min(d, seg(m, vec2(7.0, 21.0), vec2(0.0, 21.0)));
    }
    if (id == 37) { // leggings: belt, two legs
        float d = seg(m, vec2(0.0, 3.0), vec2(7.5, 3.0));
        d = min(d, seg(m, vec2(0.0, 7.0), vec2(7.5, 7.0)));
        d = min(d, seg(m, vec2(7.5, 3.0), vec2(8.0, 21.0)));
        d = min(d, seg(m, vec2(8.0, 21.0), vec2(3.0, 21.0)));
        d = min(d, seg(m, vec2(3.0, 21.0), vec2(1.2, 10.5)));
        return min(d, seg(m, vec2(1.2, 10.5), vec2(0.0, 10.5)));
    }
    if (id == 38) { // boot: shaft, toe, sole
        float d = seg(p, vec2(5.0, 3.0), vec2(12.0, 3.0));
        d = min(d, seg(p, vec2(12.0, 3.0), vec2(12.0, 12.0)));
        d = min(d, seg(p, vec2(12.0, 12.0), vec2(18.0, 13.5)));
        d = min(d, seg(p, vec2(18.0, 13.5), vec2(20.5, 16.5)));
        d = min(d, seg(p, vec2(20.5, 16.5), vec2(20.5, 20.5)));
        d = min(d, seg(p, vec2(20.5, 20.5), vec2(4.0, 20.5)));
        d = min(d, seg(p, vec2(4.0, 20.5), vec2(4.0, 13.0)));
        return min(d, seg(p, vec2(4.0, 13.0), vec2(5.0, 3.0)));
    }
    if (id == 39) return min(seg(p, vec2(5.0, 12.0), vec2(19.0, 12.0)), seg(p, vec2(12.0, 5.0), vec2(12.0, 19.0))); // plus
    if (id == 40) { // globe: outline, equator, meridian
        float d = min(ring(p, vec2(12.0), 10.0), seg(p, vec2(2.0, 12.0), vec2(22.0, 12.0)));
        return min(d, ellipse(p - vec2(12.0), vec2(4.2, 10.0)));
    }
    if (id == 41) { // server: two racks with a light each
        float d = min(abs(box(p, vec2(12.0, 6.0), vec2(10.0, 4.0), 2.0)), abs(box(p, vec2(12.0, 18.0), vec2(10.0, 4.0), 2.0)));
        return min(d, min(length(p - vec2(6.0, 6.0)), length(p - vec2(6.0, 18.0))));
    }
    if (id == 42) { // cube
        float d = seg(m, vec2(0.0, 2.0), vec2(9.0, 7.0));
        d = min(d, seg(m, vec2(9.0, 7.0), vec2(9.0, 17.0)));
        d = min(d, seg(m, vec2(9.0, 17.0), vec2(0.0, 22.0)));
        d = min(d, seg(m, vec2(9.0, 7.0), vec2(0.0, 12.0)));
        return min(d, seg(p, vec2(12.0, 12.0), vec2(12.0, 22.0)));
    }
    if (id == 43) { // pine tree
        float d = seg(m, vec2(0.0, 2.0), vec2(7.0, 10.5));
        d = min(d, seg(m, vec2(7.0, 10.5), vec2(3.5, 10.5)));
        d = min(d, seg(m, vec2(3.5, 10.5), vec2(8.0, 17.0)));
        d = min(d, seg(m, vec2(8.0, 17.0), vec2(0.0, 17.0)));
        return min(d, seg(p, vec2(12.0, 17.0), vec2(12.0, 22.0)));
    }
    if (id == 44) { // volume: speaker and two waves
        float d = seg(p, vec2(11.0, 5.0), vec2(6.0, 9.0));
        d = min(d, seg(p, vec2(6.0, 9.0), vec2(2.0, 9.0)));
        d = min(d, seg(p, vec2(2.0, 9.0), vec2(2.0, 15.0)));
        d = min(d, seg(p, vec2(2.0, 15.0), vec2(6.0, 15.0)));
        d = min(d, seg(p, vec2(6.0, 15.0), vec2(11.0, 19.0)));
        d = min(d, seg(p, vec2(11.0, 19.0), vec2(11.0, 5.0)));
        return min(d, min(arc(p, vec2(12.0), 5.0, vec2(0.8, 0.6)), arc(p, vec2(13.0, 12.0), 9.0, vec2(0.7071, 0.7071))));
    }
    if (id == 45) { // monitor
        float d = abs(box(p, vec2(12.0, 10.0), vec2(10.0, 7.0), 2.0));
        return min(d, min(seg(p, vec2(8.0, 21.0), vec2(16.0, 21.0)), seg(p, vec2(12.0, 17.0), vec2(12.0, 21.0))));
    }
    if (id == 46) { // languages: a glyph and a Latin A
        float d = seg(p, vec2(5.0, 8.0), vec2(11.0, 14.0));
        d = min(d, seg(p, vec2(4.0, 14.0), vec2(10.0, 8.0)));
        d = min(d, seg(p, vec2(10.0, 8.0), vec2(12.0, 5.0)));
        d = min(d, seg(p, vec2(2.0, 5.0), vec2(14.0, 5.0)));
        d = min(d, seg(p, vec2(7.0, 2.0), vec2(8.0, 2.0)));
        d = min(d, seg(p, vec2(22.0, 22.0), vec2(17.0, 12.0)));
        d = min(d, seg(p, vec2(17.0, 12.0), vec2(12.0, 22.0)));
        return min(d, seg(p, vec2(14.0, 18.0), vec2(20.0, 18.0)));
    }
    if (id == 47) { // snowflake: six arms with a branch each
        float d = 1e3;
        for (int i = 0; i < 6; i++) {
            float a = float(i) * 1.0471976 - 1.5707963;
            vec2 dir = vec2(cos(a), sin(a)), side = vec2(-dir.y, dir.x);
            d = min(d, seg(p, vec2(12.0), vec2(12.0) + dir * 10.0));
            d = min(d, seg(p, vec2(12.0) + dir * 5.0, vec2(12.0) + dir * 7.8 + side * 2.8));
            d = min(d, seg(p, vec2(12.0) + dir * 5.0, vec2(12.0) + dir * 7.8 - side * 2.8));
        }
        return d;
    }
    if (id == 48) { // power
        vec2 q = p - vec2(12.0, 13.0);
        float r = q.y < 0.0 && abs(q.x) < -q.y ? min(length(q - vec2(6.36, -6.36)), length(q - vec2(-6.36, -6.36))) : abs(length(q) - 9.0);
        return min(r, seg(p, vec2(12.0, 2.0), vec2(12.0, 12.0)));
    }
    if (id == 49) { // trash
        float d = seg(p, vec2(3.0, 6.0), vec2(21.0, 6.0));
        d = min(d, p.y > 6.0 ? abs(box(p, vec2(12.0, 13.0), vec2(7.0, 9.0), 2.0)) : 1e3);
        d = min(d, p.y < 6.0 ? abs(box(p, vec2(12.0, 5.0), vec2(4.0, 3.0), 2.0)) : 1e3);
        return min(d, min(seg(p, vec2(10.0, 11.0), vec2(10.0, 17.0)), seg(p, vec2(14.0, 11.0), vec2(14.0, 17.0))));
    }
    if (id == 50) { // pencil, along the diagonal
        vec2 q = p - vec2(12.5, 11.5);
        vec2 r = vec2(q.x - q.y, q.x + q.y) * 0.7071068;
        return min(abs(box(r, vec2(0.0), vec2(9.0, 2.8), 0.9)), seg(r, vec2(-4.8, -2.8), vec2(-4.8, 2.8)));
    }
    if (id == 51) { fill = true; return min(markHalf(p), markHalf(vec2(24.0 - p.x, p.y))); } // Maved mark: the M
    if (id == 56) { fill = true; return markHalf(p); } // its left peak
    if (id == 57) { fill = true; return markHalf(vec2(24.0 - p.x, p.y)); } // its right peak
    if (id == 52) { // dice: rounded square with five pips
        float d = abs(box(p, vec2(12.0), vec2(9.0), 2.5));
        d = min(d, min(length(p - vec2(8.0, 8.0)), length(p - vec2(16.0, 16.0))));
        d = min(d, min(length(p - vec2(16.0, 8.0)), length(p - vec2(8.0, 16.0))));
        return min(d, length(p - vec2(12.0)));
    }
    if (id == 53) { // music: two notes joined by a beam
        float d = min(ring(p, vec2(6.0, 18.0), 3.0), ring(p, vec2(18.0, 16.0), 3.0));
        d = min(d, min(seg(p, vec2(9.0, 18.0), vec2(9.0, 5.0)), seg(p, vec2(21.0, 16.0), vec2(21.0, 3.0))));
        return min(d, seg(p, vec2(9.0, 5.0), vec2(21.0, 3.0)));
    }
    if (id == 54) { // robot: head with antenna, two eyes and a mouth
        float d = abs(box(p, vec2(12.0, 14.0), vec2(8.0, 6.5), 2.5));
        d = min(d, seg(p, vec2(12.0, 7.5), vec2(12.0, 4.0)));
        d = min(d, length(p - vec2(12.0, 3.0)));
        d = min(d, min(length(p - vec2(9.0, 13.0)), length(p - vec2(15.0, 13.0))));
        d = min(d, seg(p, vec2(9.5, 17.0), vec2(14.5, 17.0)));
        return min(d, min(seg(p, vec2(2.0, 12.0), vec2(2.0, 16.0)), seg(p, vec2(22.0, 12.0), vec2(22.0, 16.0))));
    }
    if (id == 55) { // navigation arrow pointing up, filled, with a notch at the back
        fill = true;
        return min(tri(p, vec2(12.0, 2.0), vec2(20.5, 21.0), vec2(12.0, 16.5)), tri(p, vec2(12.0, 2.0), vec2(12.0, 16.5), vec2(3.5, 21.0)));
    }
    if (id == 58) { // gear: a ring with eight short teeth
        vec2 q = p - vec2(12.0);
        float step = 0.7853982, a = (floor(atan(q.y, q.x) / step) + 0.5) * step;
        vec2 u = vec2(cos(a), sin(a));
        return min(ring(p, vec2(12.0), 5.6), seg(q, u * 6.6, u * 9.6));
    }
    return 1e3;
}

// --- Shapes ---

// Rounded rectangle centred on the origin; corners are the UI's L4 squircle, or circular arcs.
float roundedBox(vec2 p, vec2 b, float r, bool circular) {
    vec2 q = abs(p) - (b - r);
    float outside;
    if (q.x > 0.0 && q.y > 0.0) {
        if (circular) outside = length(q);
        else {
            vec2 q2 = q * q;
            outside = sqrt(sqrt(q2.x * q2.x + q2.y * q2.y));
        }
    } else outside = max(q.x, q.y);
    return outside - r;
}
float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

void main() {
    int mode = params.x & 7;
    // Derivatives are taken for every fragment, before any branch.
    vec2 slope = fwidth(local);
    float coverage;
    if (mode == 3) {
        vec4 texel;
        vec2 size;
        if ((params.x & 16) != 0) { texel = texture(Sampler1, local); size = vec2(textureSize(Sampler1, 0)); }
        else { texel = texture(Sampler0, local); size = vec2(textureSize(Sampler0, 0)); }
        float distance = median(texel.r, texel.g, texel.b);
        vec2 unitRange = vec2(float(params.z) / 16.0) / size;
        vec2 screenTexSize = 1.0 / max(slope, vec2(0.000001));
        float screenRange = max(0.5 * dot(unitRange, screenTexSize), 1.0);
        coverage = clamp(screenRange * (distance - 0.5) + 0.5, 0.0, 1.0);
    } else if (mode == 4) {
        bool fill;
        int id = params.y;
        float stroke = float(params.z) / 16.0;
        if (id == 22) {
            stroke = float(params.z % 128) / 16.0;
            arcProgress = float(params.z / 128) / 255.0;
        }
        float pixel = max(length(slope) * 0.70710678, 1e-4);
        float d = icon(id, local, fill);
        float edge = fill ? d : d - stroke * 0.5;
        coverage = clamp(0.5 - edge / pixel, 0.0, 1.0);
    } else {
        float radius = float(params.y) / 16.0;
        float d = roundedBox(local, halfSize, radius, (params.x & 8) != 0);
        float pixel = max(length(slope) * 0.70710678, 1e-4);
        if (mode == 1) {
            float blur = max(float(params.z) / 16.0, 1e-3);
            float t = clamp(d / blur, 0.0, 1.0);
            coverage = d <= 0.0 ? 1.0 : max(exp(-4.5 * t * t) - 0.011109, 0.0) / 0.988891;
        } else if (mode == 2) {
            float width = float(params.z) / 16.0;
            coverage = clamp(1.0 - d / pixel, 0.0, 1.0) * clamp(1.0 + (d + width) / pixel, 0.0, 1.0);
        } else {
            // Square corners stay hard, as the old plain quad did; rounded ones fade over a pixel outside the edge.
            coverage = radius <= 0.0 ? 1.0 : clamp(1.0 - d / pixel, 0.0, 1.0);
        }
    }
    fragColor = vec4(vertexColor.rgb, vertexColor.a * coverage) * ColorModulator;
    if (fragColor.a <= 0.0) discard;
}
