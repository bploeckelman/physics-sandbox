package zendo.games.physics.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.StringBuilder;
import zendo.games.physics.controllers.CameraController;
import zendo.games.physics.controllers.TopDownCameraController;
import zendo.games.physics.sandbox.FreeCameraController;
import zendo.games.physics.scene.Scene;
import zendo.games.physics.scene.components.NameComponent;
import zendo.games.physics.scene.systems.RenderSystem;

import static com.badlogic.gdx.Input.Keys;
import static zendo.games.physics.scene.Components.Families;

public class EditorScreen extends BaseScreen {

    private final Scene scene;
    private final RenderSystem renderSystem;

    private CameraController cameraController;

    public EditorScreen() {
        var fov = 67f;
        var viewWidth = 1280f;
        var viewHeight = 720f;
        this.worldCamera = new PerspectiveCamera(fov, viewWidth, viewHeight);
        worldCamera.near = 0.5f;
        worldCamera.far = 1000f;
        worldCamera.position.set(-15f, 10f, -15f);
        worldCamera.lookAt(0f, 0f, 0f);
        worldCamera.update();

        this.renderSystem = new RenderSystem();
        engine.addSystem(renderSystem);
        engine.addEntityListener(Families.modelInstance, renderSystem);

        this.scene = new Scene(engine);

        this.cameraController = new TopDownCameraController(worldCamera);
        var mux = new InputMultiplexer(this, cameraController);
        Gdx.input.setInputProcessor(mux);
        Gdx.input.setCursorCatched(true);
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
            var namedEntities = engine.getEntitiesFor(Families.name);
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

    private void switchCameras() {
        if (cameraController instanceof TopDownCameraController) {
            cameraController = new FreeCameraController(worldCamera);
        } else if (cameraController instanceof FreeCameraController) {
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
                switchCameras();
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
}
