package zendo.games.physics.screens;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;
import zendo.games.physics.Config;
import zendo.games.physics.controllers.CameraController;
import zendo.games.physics.controllers.TopDownCameraController;
import zendo.games.physics.sandbox.FreeCameraController;
import zendo.games.physics.scene.Scene;
import zendo.games.physics.scene.components.NameComponent;
import zendo.games.physics.scene.components.PhysicsComponent;
import zendo.games.physics.scene.components.utils.ComponentFamilies;
import zendo.games.physics.scene.components.utils.ComponentMappers;
import zendo.games.physics.scene.systems.PhysicsSystem;
import zendo.games.physics.scene.systems.ProviderSystem;
import zendo.games.physics.scene.systems.RenderSystem;
import zendo.games.physics.scene.systems.UserInterfaceSystem;

import java.util.Objects;

import static com.badlogic.gdx.Input.Buttons;
import static com.badlogic.gdx.Input.Keys;
import static zendo.games.physics.scene.providers.CollisionShapeProvider.Type;

public class EditorScreen extends BaseScreen {

    private static final String TAG = EditorScreen.class.getSimpleName();

    private final Scene scene;

    private final ProviderSystem providerSystem;
    private final RenderSystem renderSystem;
    private final PhysicsSystem physicsSystem;
    private final UserInterfaceSystem userInterfaceSystem;

    private CameraController cameraController;
    private final OrthographicCamera orthoCamera;
    private final PerspectiveCamera perspectiveCamera;

    private final float SPAWN_TIME = 1f;
    private float spawnTimer = SPAWN_TIME;

    public EditorScreen() {
        var fov = 67f;
        var viewWidth = 1280f;
        var viewHeight = 720f;

        this.orthoCamera = new OrthographicCamera();
        this.orthoCamera.setToOrtho(false, viewWidth, viewHeight);
        this.orthoCamera.update(true);

        this.perspectiveCamera = new PerspectiveCamera(fov, viewWidth, viewHeight);
        perspectiveCamera.near = 0.5f;
        perspectiveCamera.far = 1000f;

        this.worldCamera = perspectiveCamera;

        this.providerSystem = new ProviderSystem(assets);
        engine.addSystem(providerSystem);

        this.renderSystem = new RenderSystem();
        engine.addEntityListener(ComponentFamilies.modelInstances, renderSystem);
        engine.addSystem(renderSystem);

        this.physicsSystem = new PhysicsSystem();
        engine.addEntityListener(ComponentFamilies.physics, physicsSystem);
        engine.addSystem(physicsSystem);

        this.userInterfaceSystem = new UserInterfaceSystem(assets, engine);
        // TODO - setup ui system as entity listener once there are some ui components
        engine.addSystem(userInterfaceSystem);

        this.scene = new Scene(engine);

        this.worldCamera = orthoCamera;
        this.cameraController = new TopDownCameraController(worldCamera);
        var mux = new InputMultiplexer(this, cameraController);
        Gdx.input.setInputProcessor(mux);

        // re-add the in-game console to the input multiplexer
        userInterfaceSystem.console.resetInputProcessing();
    }

    @Override
    public void dispose() {
        scene.dispose();

        for (var sys : engine.getSystems()) {
            if (sys instanceof Disposable system) {
                system.dispose();
            }
        }
        engine.removeAllSystems();
        engine.removeAllEntities();
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        if (userInterfaceSystem.commandExecutor.isObjectSpawningEnabled) {
            spawnTimer -= delta;
            if (spawnTimer <= 0f) {
                spawnTimer = SPAWN_TIME;
                // TODO - pick position in a random tile
                scene.spawnCrate();
            }
        }

        scene.update(delta);
        cameraController.update(delta);
    }

    @Override
    public void render() {
        ScreenUtils.clear(Color.SKY, true);

        if (Config.Debug.wireframe) {
            renderSystem.render(worldCamera, assets.wireframeModelBatch, null);
        } else {
            renderSystem.renderShadows(worldCamera, assets.shadowModelBatch, scene.shadowLight);
            renderSystem.render(worldCamera, assets.modelBatch, scene.env());
        }

        if (Config.Debug.physics) {
            physicsSystem.renderDebug(worldCamera);
        }

        userInterfaceSystem.render(windowCamera, assets.batch);
    }

    private void toggleCamera() {
        if (cameraController instanceof TopDownCameraController) {
            worldCamera = perspectiveCamera;
            cameraController = new FreeCameraController(worldCamera);
        } else if (cameraController instanceof FreeCameraController) {
            worldCamera = orthoCamera;
            cameraController = new TopDownCameraController(worldCamera);
        }
        var mux = new InputMultiplexer(this, cameraController);
        Gdx.input.setInputProcessor(mux);

        // restore the in-game console to the input multiplexer
        engine.getSystem(UserInterfaceSystem.class).console.resetInputProcessing();
    }

    // TESTING -------------------------------
    private int componentCount = 1;
    // TESTING -------------------------------

    @Override
    public boolean keyUp(int keycode) {
        switch (keycode) {
            case Keys.ESCAPE -> {
                Gdx.app.exit();
                return true;
            }
            case Keys.TAB -> {
                toggleCamera();
                return true;
            }
            case Keys.NUM_1 -> {
                Config.Debug.wireframe = !Config.Debug.wireframe;
                return true;
            }
            case Keys.NUM_2 -> {
                Config.Debug.physics = !Config.Debug.physics;
                return true;
            }
            case Keys.SPACE -> {
                if (worldCamera instanceof PerspectiveCamera) {
                    scene.spawnShot(worldCamera);
                }
                return true;
            }
            case Keys.F -> {
                if (worldCamera instanceof OrthographicCamera) {
                    if (editInfo.isHolding()) {
                        rotateEntityCCW(editInfo.heldEntity);
                    }
                }
                return true;
            }
            case Keys.G -> {
                if (worldCamera instanceof OrthographicCamera) {
                    if (editInfo.isHolding()) {
                        rotateEntityCW(editInfo.heldEntity);
                    }
                }
            }
            // TESTING -------------------------------
            case Keys.PLUS -> {
                engine.addEntity(engine.createEntity()
                        .add(new NameComponent("TEST" + componentCount++)));
                return true;
            }
            case Keys.DEL -> {
                var entities = engine.getEntities();
                if (entities.size() > 0) {
                    int random = MathUtils.random(0, entities.size() - 1);
                    var entity = entities.get(random);
                    engine.removeEntity(entity);
                    return true;
                }
            }
            // TESTING -------------------------------
        }
        return super.keyUp(keycode);
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        if (worldCamera instanceof OrthographicCamera && editInfo.isHolding()) {
            worldCamera.getPickRay(screenX, screenY)
                       .getEndPoint(pointerPos, worldCamera.position.y);

            // keep object positioned within a tile
            var tileSize = 10f;
            var x = MathUtils.floor(pointerPos.x / tileSize) * tileSize;
            var z = MathUtils.floor(pointerPos.z / tileSize) * tileSize;
            var offset = tileSize / 2f;
            var position = vec3Pool.obtain().set(offset + x, 0, offset + z);
            var scaling = vec3Pool.obtain().set(tileSize, tileSize, tileSize);
            {
                // update the model instance position
                var instance = ComponentMappers.modelInstance.get(editInfo.heldEntity);
                if (instance != null) {
                    instance.transform.setTranslation(position);
                }

                // update the physics body position
                var physics = ComponentMappers.physics.get(editInfo.heldEntity);
                if (physics != null) {
                    var transform = physics.rigidBody.getWorldTransform();
                    transform.setTranslation(position);
                    physics.rigidBody.setWorldTransform(transform);
                }
            }
            vec3Pool.free(position);
            vec3Pool.free(scaling);
            return true;
        }
        return super.mouseMoved(screenX, screenY);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (worldCamera instanceof PerspectiveCamera) {
            scene.spawnShot(worldCamera);
            return super.touchUp(screenX, screenY, pointer, button);
        }

        switch (button) {
            case Buttons.LEFT -> {
                if (editInfo.isHolding()) {
                    // give it a new name and then leave the held entity in the world in its current configuration
                    // TODO - store a tentative name on the thing on creation
                    editInfo.heldEntity.add(new NameComponent("tile " + componentCount++));
                    editInfo.releaseEntity();
                } else {
                    // create a new entity in the clicked tile
                    worldCamera.getPickRay(screenX, screenY)
                               .getEndPoint(pointerPos, worldCamera.position.y);

                    var tileSize = 10f;
                    var x = MathUtils.floor(pointerPos.x / tileSize) * tileSize;
                    var z = MathUtils.floor(pointerPos.z / tileSize) * tileSize;
                    var offset = tileSize / 2f;
                    var position = vec3Pool.obtain().set(offset + x, 0, offset + z);
                    var scaling = vec3Pool.obtain().set(tileSize, tileSize, tileSize);

                    // TODO - export models with a uniform scale and orientation so scaling can apply uniformly
                    // create the model instance
                    var fileName = "start.g3db"; // big
//                    var fileName = "tile-start.g3db"; // small
//                    var fileName = "straight.g3db";   // small
                    var models = providerSystem.modelProvider;
                    var model = models.getOrCreate(fileName, assets.mgr.get(fileName, Model.class));
                    Objects.requireNonNull(model, "Failed to get model file '" + fileName + "' from asset manager");

                    var instance = models.createModelInstanceComponent(fileName);
                    instance.transform.setToTranslation(position);
//                    instance.transform.setToTranslationAndScaling(position, scaling);

                    // create the physics body
                    var key = fileName.substring(1, fileName.indexOf('.')) + (componentCount + 1);
                    var shape = providerSystem.collisionShapeProvider
                            .builder(Type.custom, key).model(model).build();
                    // TODO - depends on model size since bullet's bvhTriangleMeshShape seems to have the wrong scale?
                    shape.setLocalScaling(scaling);

                    var physics = new PhysicsComponent(0f, instance.transform, shape);

                    // manage the rigidBody translation manually
                    // instead of letting bullet do it with the motion state
                    // TODO - make things like this into construction parameters
                    physics.rigidBody.setMotionState(null);

                    // set initial position and orientation of physics body
                    var transform = physics.rigidBody.getWorldTransform();
                    // TODO - model should be exported as y-up, though there might be a bullet quirk that ignores that
                    transform.rotate(Vector3.X, -90f);
                    physics.rigidBody.setWorldTransform(transform);

                    var entity = engine.createEntity()
                            .add(new NameComponent("Held Tile"))
                            .add(instance)
                            .add(physics);
                    engine.addEntity(entity);

                    editInfo.heldEntity = entity;

                    vec3Pool.free(position);
                    vec3Pool.free(scaling);
                }
                return true;
            }
            case Buttons.RIGHT -> {
                if (editInfo.isHolding()) {
                    // delete the entity from the world before releasing it
                    engine.removeEntity(editInfo.heldEntity);
                    editInfo.releaseEntity();
                }
                return true;
            }
        }

        return super.touchUp(screenX, screenY, pointer, button);
    }

    static class EditInfo {
        final Vector3 translation = new Vector3();
        final Quaternion rotation = new Quaternion();

        Entity heldEntity = null;

        public boolean isHolding() {
            return heldEntity != null;
        }
        public void releaseEntity() {
            heldEntity = null;
        }
    }
    private final EditInfo editInfo = new EditInfo();

    private void rotateEntityCW(Entity entity) {
        // update model transform
        var instance = ComponentMappers.modelInstance.get(entity);
        instance.transform.rotate(Vector3.Y, -90f);

        // update physics transform
        var physics = ComponentMappers.physics.get(entity);
        var transform = physics.rigidBody.getWorldTransform();
        transform.getTranslation(editInfo.translation);
        transform.getRotation(editInfo.rotation);
        var yAngle = editInfo.rotation.getAngleAround(Vector3.Y);
        transform.idt()
                .rotate(Vector3.Y, yAngle - 90f)
                .rotate(Vector3.X, -90f)
                .setTranslation(editInfo.translation)
        ;
        physics.rigidBody.setWorldTransform(transform);
    }

    private void rotateEntityCCW(Entity entity) {
        // update model transform
        var instance = ComponentMappers.modelInstance.get(entity);
        instance.transform.rotate(Vector3.Y, 90f);

        // update physics transform
        var physics = ComponentMappers.physics.get(entity);
        var transform = physics.rigidBody.getWorldTransform();
        transform.getTranslation(editInfo.translation);
        transform.getRotation(editInfo.rotation);
        var yAngle = editInfo.rotation.getAngleAround(Vector3.Y);
        transform.idt()
                .rotate(Vector3.Y, yAngle + 90f)
                .rotate(Vector3.X, -90f)
                .setTranslation(editInfo.translation)
        ;
        physics.rigidBody.setWorldTransform(transform);
    }

}
