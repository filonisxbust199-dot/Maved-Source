package ess.ll.client.integration;

import org.joml.Matrix4f;

public interface BaritoneVertexConsumerAccess {
   void lineVertexPre(float x, float y, float z, int color, float normalX, float normalY, float normalZ, float width);

   void lineVertex(Matrix4f matrix, float x, float y, float z, int color, float normalX, float normalY, float normalZ, float width);

   void coloredVertexPre(float x, float y, float z, int color);

   void coloredVertexF(Matrix4f matrix, float x, float y, float z, float red, float green, float blue, float alpha);

   void lineVertexF(
      Matrix4f matrix, float x, float y, float z, float red, float green, float blue, float alpha, float normalX, float normalY, float normalZ, float width
   );

   void coloredVertexFPre(float x, float y, float z, float red, float green, float blue, float alpha);

   void texturedVertex(Matrix4f matrix, float x, float y, float z, float u, float v, int color);

   void coloredVertex(Matrix4f matrix, float x, float y, float z, int color);

   void texturedVertexPre(float x, float y, float z, float u, float v, int color);
}
