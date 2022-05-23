package zendo.games.physics.screens;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.StringBuilder;
import zendo.games.physics.Game;
import zendo.games.physics.controllers.CameraController;
import zendo.games.physics.controllers.TopDownCameraController;
import zendo.games.physics.sandbox.FreeCameraController;
import zendo.games.physics.scene.Scene;
import zendo.games.physics.scene.components.ModelInstanceComponent;
import zendo.games.physics.scene.components.NameComponent;
import zendo.games.physics.scene.systems.RenderSystem;

import static com.badlogic.gdx.Input.Buttons;
import static com.badlogic.gdx.Input.Keys;
import static zendo.games.physics.scene.Components.Families;

public class EditorScreen extends BaseScreen {

    private static final String TAG = EditorScreen.class.getSimpleName();

    private final Scene scene;
    private final RenderSystem renderSystem;

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
        engine.addSystem(renderSystem);
        engine.addEntityListener(Families.modelInstanceComponents, renderSystem);

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

        // shadows ----------------------------------------
        var shadowBatch = assets.shadowModelBatch;
        scene.shadowLight.begin(Vector3.Zero, worldCamera.direction);
        shadowBatch.begin(scene.shadowLight.getCamera());
        {
            renderSystem.render(shadowBatch);
        }
        shadowBatch.end();
        scene.shadowLight.end();

        // world ------------------------------------------
        var modelBatch = assets.modelBatch;
        modelBatch.begin(worldCamera);
        {
            renderSystem.render(modelBatch, scene.env());
        }
        modelBatch.end();

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
            var namedEntities = engine.getEntitiesFor(Families.nameComponents);
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
        if (worldCamera instanceof OrthographicCamera && editInfo.isHoldingObject && editInfo.heldEntity != null) {
            var pickRay = worldCamera.getPickRay(screenX, screenY);
            pickRay.getEndPoint(pointerPos, worldCamera.position.y);

            // keep object positioned within a tile
            var tileSize = 10f;
            var x = MathUtils.floor(pointerPos.x / tileSize) * tileSize;
            var z = MathUtils.floor(pointerPos.z / tileSize) * tileSize;

            var offset = tileSize / 2f;
            var modelInstance = editInfo.heldEntity.getComponent(ModelInstanceComponent.class);
            modelInstance.transform.setToTranslation(offset + x, 0, offset + z);

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
                if (!editInfo.isHoldingObject) {
                    // create a new entity in the clicked tile
                    editInfo.isHoldingObject = true;

                    var pickRay = worldCamera.getPickRay(screenX, screenY);
                    pickRay.getEndPoint(pointerPos, worldCamera.position.y);

                    var tileSize = 10f;
                    var x = MathUtils.floor(pointerPos.x / tileSize) * tileSize;
                    var z = MathUtils.floor(pointerPos.z / tileSize) * tileSize;

                    var offset = tileSize / 2f;
                    var model = Game.instance.assets.mgr.get("start.g3db", Model.class);
                    var modelInstance = new ModelInstanceComponent(model);
                    modelInstance.transform.setToTranslation(offset + x, 0, offset + z);

                    var entity = engine.createEntity()
                            .add(new NameComponent("Held Tile"))
                            .add(modelInstance);
                    engine.addEntity(entity);

                    editInfo.heldEntity = entity;
                    return true;
                } else {
                    // already holding, place it in the scene
                    // ie. just disconnect it from the EditInfo
                    editInfo.isHoldingObject = false;
                    editInfo.heldEntity = null;
                }
            }
            case Buttons.RIGHT -> {
                editInfo.isHoldingObject = false;
                if (editInfo.heldEntity != null) {
                    engine.removeEntity(editInfo.heldEntity);
                }
                return true;
            }
        }

        return super.touchUp(screenX, screenY, pointer, button);
    }

    static class EditInfo {
        public boolean isHoldingObject = false;
        public Entity heldEntity = null;
    }
    private final EditInfo editInfo = new EditInfo();

}
