package zendo.games.physics.screens;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.StringBuilder;
import zendo.games.physics.Config;
import zendo.games.physics.Game;
import zendo.games.physics.controllers.CameraController;
import zendo.games.physics.controllers.TopDownCameraController;
import zendo.games.physics.sandbox.FreeCameraController;
import zendo.games.physics.scene.Scene;
import zendo.games.physics.scene.components.ModelInstanceComponent;
import zendo.games.physics.scene.components.NameComponent;
import zendo.games.physics.scene.components.utils.ComponentFamilies;
import zendo.games.physics.scene.components.utils.ComponentMappers;
import zendo.games.physics.scene.systems.PhysicsSystem;
import zendo.games.physics.scene.systems.RenderSystem;

import static com.badlogic.gdx.Input.Buttons;
import static com.badlogic.gdx.Input.Keys;

public class EditorScreen extends BaseScreen {

    private static final String TAG = EditorScreen.class.getSimpleName();

    private final Scene scene;

    private final RenderSystem renderSystem;
    private final PhysicsSystem physicsSystem;

    private CameraController cameraController;
    private final OrthographicCamera orthoCamera;
    private final PerspectiveCamera perspectiveCamera;

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

        this.renderSystem = new RenderSystem();
        engine.addEntityListener(ComponentFamilies.modelInstances, renderSystem);
        engine.addSystem(renderSystem);

        this.physicsSystem = new PhysicsSystem();
        engine.addEntityListener(ComponentFamilies.physics, physicsSystem);
        engine.addSystem(physicsSystem);

        this.scene = new Scene(engine);

        worldCamera = orthoCamera;
        this.cameraController = new TopDownCameraController(worldCamera);
        var mux = new InputMultiplexer(this, cameraController);
        Gdx.input.setInputProcessor(mux);
    }

    @Override
    public void dispose() {
        scene.dispose();
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        scene.update(delta);
        cameraController.update(delta);
    }

    @Override
    public void render() {
        ScreenUtils.clear(Color.SKY, true);

        if (Config.Debug.physics) {
            physicsSystem.renderDebug(worldCamera);
        } else {
            renderSystem.renderShadows(scene, worldCamera, assets.shadowModelBatch);
            renderSystem.render(worldCamera, assets.modelBatch, scene.env());
        }

        // user interface ---------------------------------
        var batch = assets.batch;
        batch.setProjectionMatrix(windowCamera.combined);
        batch.begin();
        {
            var font = assets.largeFont;
            var layout = assets.layout;

            var text = Integer.toString(Gdx.graphics.getFramesPerSecond(), 10);
            layout.setText(font, text, Color.WHITE, windowCamera.viewportWidth, Align.right, false);
            font.draw(batch, layout, 0, windowCamera.viewportHeight);

            var str = new StringBuilder();
            str.append("Entities:\n");
            var namedEntities = engine.getEntitiesFor(ComponentFamilies.names);
            for (var entity : namedEntities) {
                var component = entity.getComponent(NameComponent.class);
                str.append(" - ").append(component.name()).append("\n");
            }
            text = str.toString();
            font = assets.smallFont;
            layout.setText(font, text, Color.WHITE, windowCamera.viewportWidth, Align.left, false);
            font.draw(batch, layout, 0, windowCamera.viewportHeight);
        }
        batch.end();
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
        Gdx.input.setCursorCatched(true);
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
            // TESTING -------------------------------
            case Keys.SPACE -> {
                var name = "Test " + componentCount++;
                var entity = engine.createEntity().add(new NameComponent(name));
                engine.addEntity(entity);
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
            var instance = ComponentMappers.modelInstance.get(editInfo.heldEntity);
            if (instance != null) {
                instance.transform.setToTranslation(offset + x, 0, offset + z);
            }
            return true;
        }
        return super.mouseMoved(screenX, screenY);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (worldCamera instanceof PerspectiveCamera) {
            return super.touchUp(screenX, screenY, pointer, button);
        }

        switch (button) {
            case Buttons.LEFT -> {
                if (editInfo.isHolding()) {
                    // leave the held entity in the world in its current configuration
                    editInfo.releaseEntity();
                } else {
                    // create a new entity in the clicked tile
                    worldCamera.getPickRay(screenX, screenY)
                               .getEndPoint(pointerPos, worldCamera.position.y);

                    var tileSize = 10f;
                    var x = MathUtils.floor(pointerPos.x / tileSize) * tileSize;
                    var z = MathUtils.floor(pointerPos.z / tileSize) * tileSize;

                    var offset = tileSize / 2f;
                    var model = Game.instance.assets.mgr.get("start.g3db", Model.class);
                    var instance = new ModelInstanceComponent(model);
                    instance.transform.setToTranslation(offset + x, 0, offset + z);

                    var entity = engine.createEntity()
                            .add(new NameComponent("Held Tile"))
                            .add(instance);
                    engine.addEntity(entity);

                    editInfo.heldEntity = entity;
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
        public Entity heldEntity = null;
        public boolean isHolding() {
            return heldEntity != null;
        }
        public void releaseEntity() {
            heldEntity = null;
        }
    }
    private final EditInfo editInfo = new EditInfo();

}
