package zendo.games.physics.scene.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.StringBuilder;
import com.strongjoshua.console.GUIConsole;
import zendo.games.physics.Assets;
import zendo.games.physics.scene.components.utils.ComponentFamilies;
import zendo.games.physics.scene.components.utils.ComponentMappers;

public class UserInterfaceSystem extends EntitySystem implements Disposable {

    private final Assets assets;
    private final Engine engine;
    private final GUIConsole console;

    public UserInterfaceSystem(Assets assets, Engine engine) {
        this.assets = assets;
        this.engine = engine;
        this.console = new GUIConsole();
        console.setPosition(0, 0);
        console.setSizePercent(100, 20);
    }

    public GUIConsole getConsole() {
        return console;
    }

    @Override
    public void dispose() {
        console.dispose();
    }

    public void render(Camera camera, SpriteBatch batch) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        {
            var font = assets.largeFont;
            var layout = assets.layout;

            var text = Integer.toString(Gdx.graphics.getFramesPerSecond(), 10);
            layout.setText(font, text, Color.WHITE, camera.viewportWidth, Align.right, false);
            font.draw(batch, layout, 0, camera.viewportHeight);

            var str = new StringBuilder();
            str.append("Entities:\n");
            var namedEntities = engine.getEntitiesFor(ComponentFamilies.names);
            for (var entity : namedEntities) {
                var component = ComponentMappers.name.get(entity);
                str.append(" - ").append(component.name()).append("\n");
            }

            text = str.toString();
            font = assets.smallFont;
            layout.setText(font, text, Color.WHITE, camera.viewportWidth, Align.left, false);
            font.draw(batch, layout, 0, camera.viewportHeight);
        }
        batch.end();

        console.draw();
    }

}
