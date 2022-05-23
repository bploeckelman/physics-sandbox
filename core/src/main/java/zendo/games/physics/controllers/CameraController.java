package zendo.games.physics.controllers;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.collision.BoundingBox;

public abstract class CameraController extends InputAdapter {

    protected Camera camera;
    protected BoundingBox bounds;

    public CameraController(Camera camera) {
        this.camera = camera;
        this.bounds = null;
    }

    public void setBounds(BoundingBox bounds) {
        this.bounds = bounds;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
        resetCamera();
    }

    public abstract void resetCamera();
    public abstract void update(float delta);

}
