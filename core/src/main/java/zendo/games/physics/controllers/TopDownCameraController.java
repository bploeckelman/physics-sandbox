package zendo.games.physics.controllers;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntIntMap;

public class TopDownCameraController extends CameraController {

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
    private float degreesPerPixel = 0.1f;
    private int dragX, dragY;
    public float rotateSpeed = 0.2f;

    private final Vector3 tmp = new Vector3();
    private final float INITIAL_HEIGHT = 20f;

    public TopDownCameraController(Camera camera) {
        super(camera);
        resetCamera();
    }

    public void resetCamera() {
        camera.position.set(0, INITIAL_HEIGHT, 0);
        camera.up.set(Vector3.Z).scl(-1f);
        camera.direction.set(Vector3.Y).scl(-1f);
        camera.update(true);
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
//        if (Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)) {
//            var deltaX = -Gdx.input.getDeltaX() * degreesPerPixel;
//            var deltaY = -Gdx.input.getDeltaY() * degreesPerPixel;
//            if (camera != null) {
//                camera.direction.rotate(camera.up, deltaX);
//                tmp.set(camera.direction).crs(camera.up).nor();
//                camera.direction.rotate(tmp, deltaY);
//            }
//        }
        return false;
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

    public void update(float deltaTime) {
        if (keys.containsKey(FORWARD)) {
            tmp.set(Vector3.Z).nor().scl(-deltaTime * velocity);
            camera.position.add(tmp);
        }
        if (keys.containsKey(BACKWARD)) {
            tmp.set(Vector3.Z).nor().scl(deltaTime * velocity);
            camera.position.add(tmp);
        }
        if (keys.containsKey(STRAFE_LEFT)) {
            tmp.set(Vector3.X).nor().scl(-deltaTime * velocity);
            camera.position.add(tmp);
        }
        if (keys.containsKey(STRAFE_RIGHT)) {
            tmp.set(Vector3.X).nor().scl(deltaTime * velocity);
            camera.position.add(tmp);
        }
        if (keys.containsKey(UP)) {
            tmp.set(Vector3.Y).nor().scl(deltaTime * velocity);
            camera.position.add(tmp);
        }
        if (keys.containsKey(DOWN)) {
            tmp.set(Vector3.Y).nor().scl(-deltaTime * velocity);
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
