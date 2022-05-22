package zendo.games.physics.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.utils.Align;

public class EditorScreen extends BaseScreen {

    private static final float FOV = 67f;
    private static final float VIEW_WIDTH = 1280f;
    private static final float VIEW_HEIGHT = 720f;

    public EditorScreen() {
        this.worldCamera = new PerspectiveCamera(FOV, VIEW_WIDTH, VIEW_HEIGHT);
        // TODO - additional config (position, orientation, look at)
        this.worldCamera.update();
        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void update(float delta) {
        super.update(delta);
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
        }
        batch.end();
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            Gdx.app.exit();
            return true;
        }
        return super.keyUp(keycode);
    }
}
