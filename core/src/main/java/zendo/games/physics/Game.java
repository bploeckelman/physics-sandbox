package zendo.games.physics;

import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenManager;
import aurelienribon.tweenengine.equations.Linear;
import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;
import zendo.games.physics.screens.BaseScreen;
import zendo.games.physics.screens.EditorScreen;
import zendo.games.physics.utils.ScreenTransition;
import zendo.games.physics.utils.Time;
import zendo.games.physics.utils.accessors.*;

public class Game extends ApplicationAdapter {

    public static Game instance;

    public Assets assets;
    public Engine engine;
    public TweenManager tween;

    private OrthographicCamera camera;
    private ScreenTransition transition;
    private Screens screens;

    static class Screens implements Disposable {
        public BaseScreen current;
        public BaseScreen next;

        @Override
        public void dispose() {
            if (current != null) current.dispose();
            if (next != null) next.dispose();
        }
    }

    @Override
    public void create() {
        Game.instance = this;

        assets = new Assets();
        engine = new Engine();
        tween = new TweenManager();
        Tween.setWaypointsLimit(4);
        Tween.setCombinedAttributesLimit(4);
        Tween.registerAccessor(Color.class, new ColorAccessor());
        Tween.registerAccessor(Rectangle.class, new RectangleAccessor());
        Tween.registerAccessor(Vector2.class, new Vector2Accessor());
        Tween.registerAccessor(Vector3.class, new Vector3Accessor());
        Tween.registerAccessor(OrthographicCamera.class, new CameraAccessor());

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        transition = new ScreenTransition(assets);
        screens = new Screens();
        setScreen(new EditorScreen());
    }

    @Override
    public void dispose() {
        screens.dispose();
        transition.dispose();
        if (assets.initialized) {
            assets.dispose();
        }
    }

    public void update() {
        // update global timer
        Time.delta = Gdx.graphics.getDeltaTime();

        // update code that always runs (regardless of pause)
        screens.current.updateEvenIfPaused(Time.delta);

        // handle a pause
        if (Time.pause_timer > 0) {
            Time.pause_timer -= Time.delta;
            if (Time.pause_timer <= -0.0001f) {
                Time.delta = -Time.pause_timer;
            } else {
                // skip updates if paused
                return;
            }
        }
        Time.millis += Time.delta;
        Time.previous_elapsed = Time.elapsed_millis();

        // update systems
        camera.update();
        tween.update(Time.delta);
        engine.update(Time.delta);
        screens.current.update(Time.delta);
    }

    @Override
    public void render() {
        update();

        ScreenUtils.clear(Color.DARK_GRAY, true);
        screens.current.renderIntoFrameBuffers();

        if (screens.next == null) {
            ScreenUtils.clear(Color.DARK_GRAY, true);
            screens.current.render();
        } else {
            // TODO - should this use screens.next.windowCamera?
            transition.render(assets.batch, screens.current, screens.next, camera);
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        screens.current.resize(width, height);
        camera.setToOrtho(false, width, height);
        camera.update();
    }

    public void setScreen(BaseScreen newScreen) {
        if (screens == null) return;
        if (screens.next != null) return;
        if (transition.inProgress) return;

        if (screens.current == null) {
            screens.current = newScreen;
        } else {
            float transitionSpeed = 0.5f;

            transition.inProgress = true;
            transition.percent.setValue(0);
            Timeline.createSequence()
                    .pushPause(0.1f)
                    .push(Tween.call((i, baseTween) -> screens.next = newScreen))
                    .push(Tween.to(transition.percent, 1, transitionSpeed).target(1f).ease(Linear.INOUT))
                    .push(Tween.call((i, baseTween) -> {
                        transition.inProgress = false;
                        screens.current.dispose();
                        screens.current = screens.next;
                        screens.current.onTransitionComplete();
                        screens.next = null;
                    }))
                    .start(tween);
        }
    }

}

