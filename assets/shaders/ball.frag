uniform float u_time;
uniform vec3 u_rotation;
uniform sampler2D u_texture;
uniform vec4 u_color;

varying vec4 v_color;
varying vec2 v_texCoord;


void main()
{
    vec2 coords = v_texCoord;
    vec4 colors = texture2D(u_texture, coords) * u_color;
//    colors = vec4(v_texCoord.x, v_texCoord.y, 0, 1.);
    gl_FragColor = colors;

}

