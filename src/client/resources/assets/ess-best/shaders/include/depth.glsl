#ifndef ESS_DEPTH_GLSL
#define ESS_DEPTH_GLSL
// Match the active Blaze3D device's clip-space convention.
float essDepthToNdc(float depth) {
#if ESS_Z_ZERO_TO_ONE
    return depth;
#else
    return depth * 2.0 - 1.0;
#endif
}
float essNdcToDepth(float depth) {
#if ESS_Z_ZERO_TO_ONE
    return depth;
#else
    return depth * 0.5 + 0.5;
#endif
}
#endif
