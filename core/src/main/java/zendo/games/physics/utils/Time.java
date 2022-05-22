package zendo.games.physics.utils;

import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.Timer;

public class Time {

    private static long start_millis = 0;

    public static long millis = 0;
    public static long previous_elapsed = 0;
    public static float delta = 0;
    public static float pause_timer = 0;
    public static long millis_since_play_started = 0;

    public static void init() {
        start_millis = TimeUtils.millis();
    }

    public static long elapsed_millis() {
        return TimeUtils.timeSinceMillis(start_millis);
    }

    public static void pause_for(float time) {
        if (time >= pause_timer) {
            pause_timer = time;
        }
    }

    public static boolean on_time(float time, float timestamp) {
        return (time >= timestamp) && ((time - Time.delta) < timestamp);
    }

    public static boolean on_interval(float time, float delta, float interval, float offset) {
        return Calc.floor((time - offset - delta) / interval) < Calc.floor((time - offset) / interval);
    }

    public static boolean on_interval(float delta, float interval, float offset) {
        return Time.on_interval(Time.elapsed_millis(), delta, interval, offset);
    }

    public static boolean on_interval(float interval, float offset) {
        return Time.on_interval(Time.elapsed_millis(), Time.delta, interval, offset);
    }

    public static boolean on_interval(float interval) {
        return Time.on_interval(interval, 0);
    }

    public static boolean between_interval(float time, float interval, float offset) {
        return Calc.mod_f(time - offset, interval * 2) >= interval;
    }

    public static boolean between_interval(float interval, float offset) {
        return Time.between_interval(Time.elapsed_millis(), interval, offset);
    }

    public static boolean between_interval(float interval) {
        return Time.between_interval(interval, 0);
    }

    public static void doAfterDelay(Callback callback, float delaySeconds) {
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                callback.call();
            }
        }, delaySeconds);
    }
}
