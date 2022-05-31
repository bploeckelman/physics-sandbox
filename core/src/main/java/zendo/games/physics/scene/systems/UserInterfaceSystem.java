package zendo.games.physics.scene.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.strongjoshua.console.GUIConsole;
import zendo.games.physics.Assets;
import zendo.games.physics.scene.components.utils.ComponentFamilies;
import zendo.games.physics.screens.EditorScreen;
import zendo.games.physics.utils.ConsoleCommandExecutor;

public class UserInterfaceSystem extends EntitySystem implements Disposable {

    private final Assets assets;
    private final Engine engine;
    private final Stage stage;
    private final Skin skin;

    public final GUIConsole console;
    public final ConsoleCommandExecutor commandExecutor;

    private final Rectangle settingsPaneBoundsVisible = new Rectangle();
    private final Rectangle settingsPaneBoundsHidden = new Rectangle();

    private static class Settings {
        boolean isShown;
        MoveToAction showAction;
        MoveToAction hideAction;
        VisWindow window;

        final Rectangle boundsVisible = new Rectangle();
        final Rectangle boundsHidden  = new Rectangle();
    }
    private final Settings settings = new Settings();

    public UserInterfaceSystem(EditorScreen screen, Assets assets, Engine engine) {
        this.assets = assets;
        this.engine = engine;

        VisUI.load();
        this.skin = VisUI.getSkin();

        var viewport = new ScreenViewport(screen.windowCamera);
        this.stage = new Stage(viewport);
        populateStage();

        this.console = new GUIConsole();
        console.setPosition(0, 0);
        console.setSizePercent(100, 20);

        this.commandExecutor = new ConsoleCommandExecutor(screen);
        console.setCommandExecutor(commandExecutor);
    }

    public InputProcessor getInputProcessor() {
        return stage;
    }

    public void toggleSettings() {
        if (settings.isShown) {
            hideSettings();
        } else {
            showSettings();
        }
    }

    public void showSettings() {
        if (settings.isShown) return;

        settings.isShown = true;
        settings.showAction.reset();
        settings.window.addAction(settings.showAction);
    }

    public void hideSettings() {
        if (!settings.isShown) return;

        settings.isShown = false;
        settings.hideAction.reset();
        settings.window.addAction(settings.hideAction);
    }

    @Override
    public void dispose() {
        console.dispose();
        stage.dispose();
        VisUI.dispose();
    }

    @Override
    public void update(float delta) {
        stage.act(delta);
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

        stage.draw();

        console.draw();
    }

    public void populateStage() {
        var defaultWindowStyle = skin.get("default", Window.WindowStyle.class);
        var glassWindowStyle = new Window.WindowStyle(defaultWindowStyle);
        glassWindowStyle.background = Assets.Patch.metal.drawable;
        glassWindowStyle.titleFont = assets.font;
        glassWindowStyle.titleFontColor = Color.WHITE;

        settings.isShown = false;

        var camera = stage.getCamera();
        var percentWidth = 0.2f;
        var percentHeight = 1f;
        settings.boundsVisible.set(0f, 0f, percentWidth * camera.viewportWidth, percentHeight * camera.viewportHeight);
        settings.boundsHidden.set(settings.boundsVisible);
        settings.boundsHidden.x -= settings.boundsVisible.width;

        settings.window = new VisWindow("", glassWindowStyle);
        settings.window.pad(10);
//        settings.window.pad(40, 20, 0, 0);
        settings.window.setSize(settings.boundsHidden.width, settings.boundsHidden.height);
        settings.window.setPosition(settings.boundsHidden.x, settings.boundsHidden.y);
        settings.window.align(Align.top | Align.center);
        settings.window.setModal(false);
        settings.window.setMovable(false);
        settings.window.setKeepWithinStage(false);

        var showDuration = 0.075f;
        var hideDuration = 0.05f;

        settings.showAction = new MoveToAction();
        settings.showAction.setPosition(settings.boundsVisible.x, settings.boundsVisible.y);
        settings.showAction.setDuration(showDuration);

        settings.hideAction = new MoveToAction();
        settings.hideAction.setPosition(settings.boundsHidden.x, settings.boundsHidden.y);
        settings.hideAction.setDuration(hideDuration);

        var scrollTable = new VisTable();
        for (int i = 0; i < 100; i++) {
            scrollTable.add(new VisLabel("Item " + i));
            scrollTable.row();
        }
        var scrollPane = new VisScrollPane(scrollTable);
        scrollPane.setFillParent(true);

        settings.window.addActor(scrollPane);

        stage.addActor(settings.window);
    }

}
