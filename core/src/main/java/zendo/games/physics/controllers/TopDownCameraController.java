package zendo.games.physics.controllers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntIntMap;
import zendo.games.physics.utils.Calc;
import zendo.games.physics.utils.Time;

public class TopDownCameraController extends CameraController {

    private static final String TAG = TopDownCameraController.class.getSimpleName();

    private final float SPEED_0 = 10f;
    private final float SPEED_1 = 100f;
    private final float SPEED_2 = 500f;

    private final IntIntMap keys = new IntIntMap();
    private final Vector3 direction = new Vector3();

    private final int STRAFE_LEFT = Input.Keys.A;
    private final int STRAFE_RIGHT = Input.Keys.D;
    private final int FORWARD = Input.Keys.W;
    private final int BACKWARD = Input.Keys.S;
    private final int UP = Input.Keys.Q;
    private final int DOWN = Input.Keys.E;

    private float velocity = SPEED_0;

    // TODO - scale mouse pan speed based on zoom level
    private float unitsDraggedPerPixel = 5f;

    private final float ZOOM_INITIAL = 0.075f;
    private final float ZOOM_MIN = 0.01f;
    private final float ZOOM_MAX = 1f;
    private final float ZOOM_SCALE_MIN = 0.01f;
    private final float ZOOM_SCALE_MAX = 2f;
    private float zoomConstantScale = ZOOM_SCALE_MIN;

    private final Vector3 tmp = new Vector3();

    public TopDownCameraController(Camera camera) {
        super(camera);
        resetCamera();
    }

    public void resetCamera() {
        camera.position.set(0, 1, 0);
        camera.up.set(Vector3.Z).scl(-1f);
        camera.direction.set(Vector3.Y).scl(-1f);
        if (camera instanceof OrthographicCamera ortho) {
            ortho.zoom = ZOOM_INITIAL;
        }
        camera.update(true);
        Gdx.input.setCursorCatched(false);
    }

    @Override
    public boolean keyDown(int keycode) {
        keys.put(keycode, keycode);
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        keys.remove(keycode, 0);
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)) {
            var amountX = Gdx.input.getDeltaX() * Time.delta * unitsDraggedPerPixel;
            var amountY = Gdx.input.getDeltaY() * Time.delta * unitsDraggedPerPixel;
            if (amountX > 0) moveLeft(amountX);
            if (amountX < 0) moveRight(amountX);
            if (amountY > 0) moveForward(amountY);
            if (amountY < 0) moveBack(amountY);
            return true;
        }
        return super.touchDragged(screenX, screenY, pointer);
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
//            direction.set(camera.direction);
//
//            // rotating on the y axis
//            float x = dragX -screenX;
//            // change this Vector3.y with camera.up if you have a dynamic up.
//            camera.rotate(Vector3.Y,x * rotateSpeed);
//
//            // rotating on the x and z axis is different
//            float y = (float) Math.sin( (double)(dragY -screenY)/180f);
//            if (Math.abs(camera.direction.y + y * (rotateSpeed*5.0f))< 0.9) {
//                camera.direction.y +=  y * (rotateSpeed*5.0f) ;
//            }
//
//            camera.update();
//            dragX = screenX;
//            dragY = screenY;
//            return true;
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (amountY != 0 && camera instanceof OrthographicCamera ortho) {
            var sign = Calc.sign(amountY);
            var zoom = ortho.zoom + sign * Time.delta * velocity * zoomConstantScale;
            ortho.zoom = MathUtils.clamp(zoom, ZOOM_MIN, ZOOM_MAX);
            return true;
        }
        return super.scrolled(amountX, amountY);
    }

    @Override
    public void update(float deltaTime) {
        var moveAmount = deltaTime * velocity;
        if (keys.containsKey(FORWARD))      moveForward(moveAmount);
        if (keys.containsKey(BACKWARD))     moveBack(moveAmount);
        if (keys.containsKey(STRAFE_LEFT))  moveLeft(moveAmount);
        if (keys.containsKey(STRAFE_RIGHT)) moveRight(moveAmount);

        // scale zoom amount based on distance to the ground plane
        if (camera instanceof OrthographicCamera ortho) {
            var distance = (ortho.zoom - ZOOM_MIN) / (ZOOM_MAX - ZOOM_MIN);
            zoomConstantScale = Interpolation.exp5.apply(ZOOM_SCALE_MIN, ZOOM_SCALE_MAX, distance);
            // Gdx.app.log(TAG, "zoom scale: " + zoomConstantScale);

            if (keys.containsKey(UP)) {
                var newZoom = ortho.zoom + deltaTime * velocity * zoomConstantScale;
                ortho.zoom = Calc.clamp_f(newZoom, ZOOM_MIN, ZOOM_MAX);
            }
            if (keys.containsKey(DOWN)) {
                var newZoom = ortho.zoom - deltaTime * velocity * zoomConstantScale;
                ortho.zoom = Calc.clamp_f(newZoom, ZOOM_MIN, ZOOM_MAX);
            }
        }

        camera.update(true);
    }

    /**
     * Sets the velocity in units per second for moving forward, backward and
     * strafing left/right.
     *
     * @param velocity the velocity in units per second
     */
    public void setVelocity(float velocity) {
        this.velocity = velocity;
    }

    private void moveLeft(float amount) {
        // make amount sign independent
        if (amount < 0) amount *= -1f;

        tmp.set(Vector3.X).nor().scl(-amount);
        camera.position.add(tmp);
    }

    private void moveRight(float amount) {
        // make amount sign independent
        if (amount < 0) amount *= -1f;

        tmp.set(Vector3.X).nor().scl(amount);
        camera.position.add(tmp);
    }

    private void moveForward(float amount) {
        // make amount sign independent
        if (amount < 0) amount *= -1f;

        tmp.set(Vector3.Z).nor().scl(-amount);
        camera.position.add(tmp);
    }

    private void moveBack(float amount) {
        // make amount sign independent
        if (amount < 0) amount *= -1f;

        tmp.set(Vector3.Z).nor().scl(amount);
        camera.position.add(tmp);
    }

}
