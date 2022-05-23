package zendo.games.physics.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntIntMap;
import zendo.games.physics.controllers.CameraController;

/**
 * @author Marcus Brummer
 * @version 24-11-2015
 */
public class FreeCameraController extends CameraController {

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

    // TODO - don't allow slight roll with middle mouse

    private float velocity = SPEED_0;
    private float degreesPerPixel = 0.1f;
    private int dragX, dragY;
    public float rotateSpeed = 0.2f;

    private final Vector3 tmp = new Vector3();

    public FreeCameraController(Camera camera) {
        super(camera);
        resetCamera();
    }

    @Override
    public void resetCamera() {
        camera.near = 0.5f;
        camera.far = 1000f;
        camera.position.set(-15f, 10f, -15f);
        camera.up.set(Vector3.Y);
        camera.lookAt(0f, 0f, 0f);
        camera.update(true);
        Gdx.input.setCursorCatched(true);
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

//    @Override
//    public boolean touchDragged(int screenX, int screenY, int pointer) {
//        if (Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)) {
//            var deltaX = -Gdx.input.getDeltaX() * degreesPerPixel;
//            var deltaY = -Gdx.input.getDeltaY() * degreesPerPixel;
//            if (camera != null) {
//                camera.direction.rotate(camera.up, deltaX);
//                tmp.set(camera.direction).crs(camera.up).nor();
//                camera.direction.rotate(tmp, deltaY);
//            }
//        }
//        return false;
//    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        direction.set(camera.direction);

        // rotating on the y axis
        float x = dragX -screenX;
        // change this Vector3.y with camera.up if you have a dynamic up.
        camera.rotate(Vector3.Y,x * rotateSpeed);

        // rotating on the x and z axis is different
        float y = (float) Math.sin( (double)(dragY -screenY)/180f);
        if (Math.abs(camera.direction.y + y * (rotateSpeed*5.0f))< 0.9) {
            camera.direction.y +=  y * (rotateSpeed*5.0f) ;
        }

        camera.update();
        dragX = screenX;
        dragY = screenY;
        return true;
    }

    public void update(float deltaTime) {
        if (keys.containsKey(FORWARD)) {
            tmp.set(camera.direction).nor().scl(deltaTime * velocity);
            camera.position.add(tmp);
        }
        if (keys.containsKey(BACKWARD)) {
            tmp.set(camera.direction).nor().scl(-deltaTime * velocity);
            camera.position.add(tmp);
        }
        if (keys.containsKey(STRAFE_LEFT)) {
            tmp.set(camera.direction).crs(camera.up).nor().scl(-deltaTime * velocity);
            camera.position.add(tmp);
        }
        if (keys.containsKey(STRAFE_RIGHT)) {
            tmp.set(camera.direction).crs(camera.up).nor().scl(deltaTime * velocity);
            camera.position.add(tmp);
        }
        if (keys.containsKey(UP)) {
            tmp.set(camera.up).nor().scl(deltaTime * velocity);
            camera.position.add(tmp);
        }
        if (keys.containsKey(DOWN)) {
            tmp.set(camera.up).nor().scl(-deltaTime * velocity);
            camera.position.add(tmp);
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

    /**
     * Sets how many degrees to rotate per pixel the mouse moved.
     *
     * @param degreesPerPixel
     */
    public void setDegreesPerPixel(float degreesPerPixel) {
        this.degreesPerPixel = degreesPerPixel;
    }

}