package zendo.games.physics.screens;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.StringBuilder;
import zendo.games.physics.scene.Components;
import zendo.games.physics.scene.Scene;

import static com.badlogic.gdx.Input.Keys;

public class EditorScreen extends BaseScreen {

    private static final float FOV = 67f;
    private static final float VIEW_WIDTH = 1280f;
    private static final float VIEW_HEIGHT = 720f;

    private final Scene scene;

    public EditorScreen() {
        this.worldCamera = new PerspectiveCamera(FOV, VIEW_WIDTH, VIEW_HEIGHT);
        // TODO - additional config (position, orientation, look at)
        this.worldCamera.update();

        this.scene = new Scene(engine);

        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        scene.update(delta);
    }

    @Override
    public void render() {
        // user interface
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
            var namedEntities = engine.getEntitiesFor(Components.names);
            for (var entity : namedEntities) {
                var component = entity.getComponent(Components.Name.class);
                str.append(" - ").append(component.name()).append("\n");
            }
            text = str.toString();
            layout.setText(font, text, Color.WHITE, windowCamera.viewportWidth, Align.left, false);
            font.draw(batch, layout, 0, windowCamera.viewportHeight);
        }
        batch.end();
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
            // TESTING -------------------------------
            case Keys.SPACE -> {
                engine.addEntity(new Entity()
                        .add(new Components.Name("Test " + componentCount++))
                );
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
