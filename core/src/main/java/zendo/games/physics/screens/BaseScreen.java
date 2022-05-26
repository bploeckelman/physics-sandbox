package zendo.games.physics.screens;

import aurelienribon.tweenengine.TweenManager;
import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;
import zendo.games.physics.Assets;
import zendo.games.physics.Config;
import zendo.games.physics.Game;

public abstract class BaseScreen implements InputProcessor, ControllerListener, Disposable {

    private static final String TAG = BaseScreen.class.getSimpleName();

    public static final Pool<Vector3> vec3Pool = Pools.get(Vector3.class);

    public final Game game;
    public final Assets assets;
    public final Engine engine;
    public final TweenManager tween;
    public final Vector3 pointerPos;

    public Camera worldCamera;
    public OrthographicCamera windowCamera;

    public BaseScreen() {
        this.game = Game.instance;
        this.assets = game.assets;
        this.engine = game.engine;
        this.tween = game.tween;
        this.pointerPos = new Vector3();

        this.windowCamera = new OrthographicCamera();
        this.windowCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        this.windowCamera.update();

        var camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        this.worldCamera = camera;
    }

    public abstract void render();

    public void update(float delta) {
        windowCamera.update();
        worldCamera.update();
    }

    public void resize(int width, int height) {
        windowCamera.setToOrtho(false, width, height);
        windowCamera.update();
    }

    @Override
    public void dispose() {}

    public void onTransitionComplete() {}
    public void updateEvenIfPaused(float delta) {}
    public void renderIntoFrameBuffers() {}

    // ------------------------------------------------------------------------
    // InputProcessor default implementation (from InputAdapter)
    // ------------------------------------------------------------------------

    @Override
    public boolean keyDown (int keycode) {
        return false;
    }

    @Override
    public boolean keyUp (int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped (char character) {
        return false;
    }

    @Override
    public boolean touchDown (int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp (int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged (int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved (int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled (float amountX, float amountY) {
        return false;
    }

    // ------------------------------------------------------------------------
    // ControllerListener default implementation (from ControllerAdapter)
    // ------------------------------------------------------------------------

    @Override
    public boolean buttonDown (Controller controller, int buttonIndex) {
        if (Config.Debug.general) {
            Gdx.app.log(TAG, "controller " + controller.getName() + " button " + buttonIndex + " down");
        }
        return false;
    }

    @Override
    public boolean buttonUp (Controller controller, int buttonIndex) {
        if (Config.Debug.general) {
            Gdx.app.log(TAG, "controller " + controller.getName() + " button " + buttonIndex + " up");
        }
        return false;
    }

    @Override
    public boolean axisMoved (Controller controller, int axisIndex, float value) {
        var deadzone = 0.2f;
        if (Config.Debug.general && Math.abs(value) > deadzone) {
            Gdx.app.log(TAG, "controller " + controller.getName() + " axis " + axisIndex + " moved " + value);
        }
        return false;
    }

    @Override
    public void connected (Controller controller) {
        Gdx.app.log(TAG, "controller connected: '" + controller.getName() + "' id:" + controller.getUniqueId());
    }

    @Override
    public void disconnected (Controller controller) {
        // TODO - pause game and wait for reconnect or confirmation?
        Gdx.app.log(TAG, "controller disconnected: '" + controller.getName() + "' id:" + controller.getUniqueId());
    }

}
