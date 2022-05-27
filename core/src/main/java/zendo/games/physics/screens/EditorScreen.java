package zendo.games.physics.screens;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;
import zendo.games.physics.Config;
import zendo.games.physics.controllers.CameraController;
import zendo.games.physics.controllers.TopDownCameraController;
import zendo.games.physics.controllers.FreeCameraController;
import zendo.games.physics.scene.Scene;
import zendo.games.physics.scene.components.Coord2Component;
import zendo.games.physics.scene.components.NameComponent;
import zendo.games.physics.scene.components.utils.ComponentFamilies;
import zendo.games.physics.scene.components.utils.ComponentMappers;
import zendo.games.physics.scene.factories.EntityFactory;
import zendo.games.physics.scene.systems.PhysicsSystem;
import zendo.games.physics.scene.systems.ProviderSystem;
import zendo.games.physics.scene.systems.RenderSystem;
import zendo.games.physics.scene.systems.UserInterfaceSystem;

import static com.badlogic.gdx.Input.Buttons;
import static com.badlogic.gdx.Input.Keys;

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
    private final EditInfo editInfo;

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

        this.editInfo = new EditInfo();

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

        engine.removeAllEntities();
        for (var sys : engine.getSystems()) {
            if (sys instanceof Disposable system) {
                system.dispose();
            }
        }
        engine.removeAllSystems();
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        if (userInterfaceSystem.commandExecutor.isObjectSpawningEnabled) {
            spawnTimer -= delta;
            if (spawnTimer <= 0f) {
                spawnTimer = SPAWN_TIME;
                EntityFactory.createCrate(engine);
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
                    EntityFactory.createShot(engine, worldCamera);
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
            EntityFactory.createShot(engine, worldCamera);
            return super.touchUp(screenX, screenY, pointer, button);
        }

        switch (button) {
            case Buttons.LEFT -> {
                if (editInfo.isHolding()) {
                    var entity = editInfo.heldEntity;

                    entity.add(new NameComponent("tile " + componentCount++));

                    // TODO - make sure the position is not occupied
                    // update coord component to new position
                    var modelInstance = ComponentMappers.modelInstance.get(entity);
                    var translation = vec3Pool.obtain();
                    modelInstance.transform.getTranslation(translation);
                    entity.add(new Coord2Component(
                            MathUtils.floor(translation.x / EntityFactory.TILE_SIZE),
                            MathUtils.floor(translation.z / EntityFactory.TILE_SIZE)
                    ));

                    // restore material
                    for (var material : modelInstance.materials) {
                        // remove blending
                        material.remove(BlendingAttribute.Type);

                        // restore diffuse color
                        for (var original : editInfo.originalMaterials) {
                            if (original.id.equals(material.id)) {
                                material.set(original.get(ColorAttribute.class, ColorAttribute.Diffuse));
                            }
                        }
                    }
                    editInfo.originalMaterials.clear();

                    // leave the held entity in the world in its current configuration
                    editInfo.releaseEntity();
                } else {
                    // find the tile coords on the ground plane for this pick ray
                    worldCamera.getPickRay(screenX, screenY).getEndPoint(pointerPos, worldCamera.position.y);
                    var tileX = MathUtils.floor(pointerPos.x / EntityFactory.TILE_SIZE);
                    var tileZ = MathUtils.floor(pointerPos.z / EntityFactory.TILE_SIZE);

                    // check whether the pick tile is already occupied
                    var isTileEmpty = true;
                    var coordEntities = engine.getEntitiesFor(ComponentFamilies.coord2);
                    for (var entity : coordEntities) {
                        var coord = ComponentMappers.coord2.get(entity);
                        if (coord.equals(tileX, tileZ)) {
                            // select this tile instead of creating a new one
                            editInfo.heldEntity = entity;
                            isTileEmpty = false;
                            break;
                        }
                    }

                    if (isTileEmpty) {
                        editInfo.heldEntity = EntityFactory.createTile(engine, assets, tileX, tileZ);
                    }

                    // set to selection material
                    if (editInfo.isHolding()) {
                        var entity = editInfo.heldEntity;
                        var modelInstance = ComponentMappers.modelInstance.get(entity);

                        // save original materials
                        editInfo.originalMaterials.clear();
                        for (var material : modelInstance.materials) {
                            editInfo.originalMaterials.add(material.copy());
                        }

                        // modify held entity materials
                        for (var material : modelInstance.materials) {
                            material.get(ColorAttribute.class, ColorAttribute.Diffuse).color.set(Color.WHITE);
                            material.set(new BlendingAttribute(0.75f));
                        }
                    }
                }
                return true;
            }
            case Buttons.RIGHT -> {
                if (editInfo.isHolding()) {
                    // restore material
                    var modelInstance = ComponentMappers.modelInstance.get(editInfo.heldEntity);
                    for (var material : modelInstance.materials) {
                        // remove blending
                        material.remove(BlendingAttribute.Type);

                        // restore diffuse color
                        for (var original : editInfo.originalMaterials) {
                            if (original.id.equals(material.id)) {
                                material.set(original.get(ColorAttribute.class, ColorAttribute.Diffuse));
                            }
                        }
                    }
                    editInfo.originalMaterials.clear();

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
        Array<Material> originalMaterials = new Array<>();

        public boolean isHolding() {
            return heldEntity != null;
        }
        public void releaseEntity() {
            heldEntity = null;
        }
    }

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
