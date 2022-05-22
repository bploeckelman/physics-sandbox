#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

uniform float u_time;
uniform sampler2D u_texture;

varying LOWP vec4 v_color;
varying vec2 v_texCoords;

void main()
{
    vec2 coords = v_texCoords;
    vec4 colors = texture2D(u_texture, coords);

    vec4 output_color = vec4(1.,1.,1.,1.);
    if (colors.g < u_time) {
        output_color = vec4(vec3(1. - colors.r * u_time), 1.0) * v_color;
    }

    gl_FragColor = output_color;
}
