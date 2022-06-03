package zendo.games.physics.scene.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisImageTextButton;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.strongjoshua.console.GUIConsole;
import zendo.games.physics.Assets;
import zendo.games.physics.scene.components.utils.ComponentFamilies;
import zendo.games.physics.scene.packs.MinigolfModels;
import zendo.games.physics.screens.EditorScreen;
import zendo.games.physics.utils.ConsoleCommandExecutor;

public class UserInterfaceSystem extends EntitySystem implements Disposable {

    private final Assets assets;
    private final Engine engine;
    private final Stage stage;
    private final Skin skin;

    private final Array<FrameBuffer> buttonFbos = new Array<>();
    private final Array<TextureRegion> buttonRegions = new Array<>();

    public final GUIConsole console;
    public final ConsoleCommandExecutor commandExecutor;

    public VisImageTextButton activeModelButton;

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

        this.console = new GUIConsole();
        console.setPosition(0, 0);
        console.setSizePercent(100, 20);

        this.commandExecutor = new ConsoleCommandExecutor(screen);
        console.setCommandExecutor(commandExecutor);

        prepModelsForMenu(assets.modelBatch, engine);

        populateStage();
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
        buttonFbos.forEach(FrameBuffer::dispose);

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

    private void prepModelsForMenu(ModelBatch batch, Engine engine) {
        var camera = new PerspectiveCamera(67f, 100, 100);
        camera.near = 0.1f;
        camera.far = 100f;
        camera.position.set(0f, 1f, 1f);
        camera.lookAt(Vector3.Zero);
        camera.update();

        var providers = engine.getSystem(ProviderSystem.class);
        var models = providers.modelProvider;

        // NOTE
        //   this is kind of nuts, can't find a way to make a copy of the color buffer from the fbo
        //   without that (or some additional post processing to pack all these textures into an atlas)
        //   we have to make a new fbo for each model and keep it around for the menu's lifetime
        //   because disposing the fbo destroys the pixel data in the color buffer and I don't know how to persist it
        for (var modelType : MinigolfModels.values()) {
            var fbo = new FrameBuffer(Pixmap.Format.RGBA8888, 100, 100, false);
            buttonFbos.add(fbo);

            var texture = fbo.getColorBufferTexture();
            var region = new TextureRegion(texture);
            region.flip(false, true);
            buttonRegions.add(region);

            var key = modelType.key();
            var model = models.getOrCreate(key, assets);
            var instance = new ModelInstance(model);
            instance.transform.rotate(Vector3.Y, -45f);

            fbo.begin();
            {
                ScreenUtils.clear(0f, 0f, 0f, 0f);
                batch.begin(camera);
                batch.render(instance);
                batch.end();
            }
            fbo.end();
        }
    }

    private void populateStage() {
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

        // populate a vertical scroll pane with buttons corresponding to each available model type
        var scrollTable = new VisTable();
        {
            // radio button style, only 1 checked at a time
            var group = new ButtonGroup<VisImageTextButton>();
            group.setMinCheckCount(0);
            group.setMaxCheckCount(1);

            int i = 0;
            boolean first = true;
            for (var modelType : MinigolfModels.values()) {
                var region = buttonRegions.get(i++);
                var iconDrawable = new TextureRegionDrawable(region);

                // configure a custom button style that sets the checked state to a different color than the rest
                var originalStyle = VisUI.getSkin().get(VisImageTextButton.VisImageTextButtonStyle.class);
                var style = new VisImageTextButton.VisImageTextButtonStyle(originalStyle);
                style.checked = new TextureRegionDrawable(assets.pixelRegion).tint(Color.GRAY);
                style.imageUp = iconDrawable;
                style.imageDown = iconDrawable;
                style.imageOver = iconDrawable;
                style.imageChecked = iconDrawable;
                style.imageDisabled = iconDrawable;
                style.imageCheckedOver = iconDrawable;

                var button = new VisImageTextButton(modelType.modelName(), style);
                button.setUserObject(modelType);

                final var thisButton = button;
                button.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        activeModelButton = thisButton;
                    }
                });

                // start with one button checked
                if (first) {
                    first = false;
                    button.setChecked(true);
                    activeModelButton = button;
                }

                group.add(button);

                scrollTable.add(button).growX().row();
            }
        }
        var scrollPane = new VisScrollPane(scrollTable);
        scrollPane.setFillParent(true);

        settings.window.addActor(scrollPane);

        stage.addActor(settings.window);
    }

}
