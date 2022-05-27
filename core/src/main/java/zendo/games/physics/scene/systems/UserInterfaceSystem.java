package zendo.games.physics.scene.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.strongjoshua.console.GUIConsole;
import zendo.games.physics.Assets;
import zendo.games.physics.scene.components.utils.ComponentFamilies;
import zendo.games.physics.utils.ConsoleCommandExecutor;

public class UserInterfaceSystem extends EntitySystem implements Disposable {

    private final Assets assets;
    private final Engine engine;

    public final GUIConsole console;
    public final ConsoleCommandExecutor commandExecutor;

    public UserInterfaceSystem(Assets assets, Engine engine) {
        this.assets = assets;
        this.engine = engine;
        this.console = new GUIConsole();
        console.setPosition(0, 0);
        console.setSizePercent(100, 20);

        this.commandExecutor = new ConsoleCommandExecutor();
        console.setCommandExecutor(commandExecutor);
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

            font = assets.smallFont;
            var namedEntities = engine.getEntitiesFor(ComponentFamilies.names);
            text = "Entities: " + namedEntities.size();
            layout.setText(font, text, Color.WHITE, camera.viewportWidth, Align.left, false);
            font.draw(batch, layout, 0, camera.viewportHeight);
        }
        batch.end();

        console.draw();
    }

}
