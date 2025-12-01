#version 150

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

layout(std140) uniform HudProgressParams {
    mat4 u_LocalMat;
    vec4 u_ProgressData;  // (halfWidth, halfHeight, radius, progress)
    vec3 u_GradientOffsets;  // (gradientLength, rightOffset, unused)
    mat4 u_GradientColors;  // (fillColorLeft, fillColorRight, bgColor, unused)
    float u_TransitionBorderRate;
};

in vec3 Position;
in vec4 Color;

out vec2 f_Position;
out vec4 f_Color;

void main() {
    f_Position = Position.xy;
    f_Color = Color;

    vec4 localPos = u_LocalMat * vec4(Position, 1.0);
    gl_Position = ProjMat * ModelViewMat * vec4(localPos.xy, Position.z, 1.0);
}