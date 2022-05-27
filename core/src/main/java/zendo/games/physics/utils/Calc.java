package zendo.games.physics.utils;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class Calc {

    /**
     * Given the specified angle in degrees,
     * transform the sin of that angle by the specified parameters
     */
    public static float sin_deg_xform(float angle, float restPos, float amplitude, float frequency, float phase) {
        return restPos + amplitude * MathUtils.sinDeg(frequency * (angle - phase));
    }

    /**
     * Given the specified angle in degrees,
     * transform the cos of that angle by the specified parameters
     */
    public static float cos_deg_xform(float angle, float restPos, float amplitude, float frequency, float phase) {
        return restPos + amplitude * MathUtils.cosDeg(frequency * (angle - phase));
    }

    /**
     * Given a line segment (x0,y0)..(x1,y1) and an x value,
     * return the y value such that (x,y) is on the line
     */
    public static float linear_remap_f(float x, float x0, float y0, float x1, float y1) {
        float dx = (x1 - x0);
        float dy = (y1 - y0);
        if (dx == 0) {
            return 0;
        }
        return (x - x0) / dx * dy + y0;
    }

    /**
     * Given a line segment (x0,y0)..(x1,y1) and an x value,
     * return the y value such that (x,y) is on the line
     */
    public static int linear_remap_i(int x, int x0, int y0, int x1, int y1) {
        int dx = (x1 - x0);
        int dy = (y1 - y0);
        if (dx == 0) {
            return 0;
        }
        return (x - x0) / dx * dy + y0;
    }

    public static float mod_f(float x, float m) {
        return x - (int)(x / m) * m;
    }

    public static int clamp_i(int t, int min, int max) {
        if      (t < min) return min;
        else if (t > max) return max;
        else              return t;
    }

    public static float clamp_f(float t, float min, float max) {
        if      (t < min) return min;
        else if (t > max) return max;
        else              return t;
    }

    public static float floor(float value) {
        return MathUtils.floor(value);
    }

    public static float ceil(float value) {
        return MathUtils.ceil(value);
    }

    public static float min(float a, float b) {
        return (a < b) ? a : b;
    }

    public static float max(float a, float b) {
        return (a > b) ? a : b;
    }

    public static int min(int a, int b) {
        return (a < b) ? a : b;
    }

    public static int max(int a, int b) {
        return (a > b) ? a : b;
    }

    public static float approach(float t, float target, float delta) {
        return (t < target) ? min(t + delta, target) : max(t - delta, target);
    }

    /**
     * Linear interpolation
     */
    public static float lerp(float a, float b, float t) {
        return a * (1f - t) + b * t;
    }

    /**
     * Exponential interpolation (better for multiplicative quantities, like zooming)
     */
    public static float eerp(float a, float b, float t) {
        return (float) (Math.pow(a, 1f - t) * Math.pow(b, t));
    }

    /**
     * Return the 't' value [0,1] based on the given value between a and b
     */
    public static float inv_lerp(float a, float b, float value) {
        return (value - a) / (b - a);
    }

    /**
     * Re-map a value from the input range to the output range
     * eg.
     * - alternative for HSV color ramp in health bars
     *     remap(low_health, safe_health, red_bits, green_bits, current_health) = healthbar_color_bits
     * - explosion damage radius: remap(worst_dist, safe_dist, min_damage, safe_damage, current_dist) = damage_amount
     * - stats based on level (maybe don't clamp like the
     */
    public static float remap(float inMin, float inMax, float outMin, float outMax, float value) {
        float t = inv_lerp(inMin, inMax, value);
        return lerp(outMin, outMax, t);
    }

    public static int sign(int val) {
        return (val < 0) ? -1
             : (val > 0) ? 1
             : 0;
    }

    public static float sign(float val) {
        return (val < 0) ? -1
             : (val > 0) ? 1
             : 0;
    }

    public static int abs(int val) {
        return (val < 0) ? -val : val;
    }

    public static Vector2 projection(Vector2 of, Vector2 onto, Vector2 out) {
        var dot = of.dot(onto);
        var len2 = onto.len2();
        var scl = dot / len2;
        out.set(onto).scl(scl);
        return out;
    }

}
