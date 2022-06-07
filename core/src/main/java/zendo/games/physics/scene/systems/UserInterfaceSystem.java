package zendo.games.physics.scene.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;
import com.strongjoshua.console.GUIConsole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import zendo.games.physics.Assets;
import zendo.games.physics.scene.components.TileComponent;
import zendo.games.physics.scene.components.utils.ComponentFamilies;
import zendo.games.physics.scene.components.utils.ComponentMappers;
import zendo.games.physics.scene.factories.EntityFactory;
import zendo.games.physics.scene.packs.MinigolfModels;
import zendo.games.physics.screens.EditorScreen;
import zendo.games.physics.utils.ConsoleCommandExecutor;

import java.nio.charset.StandardCharsets;

public class UserInterfaceSystem extends EntitySystem implements Disposable {

    private static final String TAG = UserInterfaceSystem.class.getSimpleName();
    
    private final Assets assets;
    private final Engine engine;
    private final Stage stage;
    private final Skin skin;
    public final GUIConsole console;
    public final ConsoleCommandExecutor commandExecutor;

    private TextureAtlas iconAtlas;

    private VisList<String> levelFileList;
    private MoveToAction showLevelFilePicker;
    private MoveToAction hideLevelFilePicker;

    public VisImageTextButton activeModelButton;

    private static class Settings {
        boolean isShown;

        VisWindow window;
        MoveToAction windowShowAction;
        MoveToAction windowHideAction;

        VisImageButton toggleButton;
        MoveToAction buttonShowAction;
        MoveToAction buttonHideAction;

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

        createIconAtlasFromModels(assets.modelBatch, engine);

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
        settings.windowShowAction.reset();
        settings.window.addAction(settings.windowShowAction);

        settings.buttonShowAction.reset();
        settings.toggleButton.addAction(settings.buttonShowAction);
    }

    public void hideSettings() {
        if (!settings.isShown) return;

        settings.isShown = false;
        settings.windowHideAction.reset();
        settings.window.addAction(settings.windowHideAction);

        settings.buttonHideAction.reset();
        settings.toggleButton.addAction(settings.buttonHideAction);
    }

    @Override
    public void dispose() {
        iconAtlas.dispose();
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

    // TODO - could try drawing all to one texture and splitting like a texture atlas,
    //  maybe reposition the camera in front of the model each time so that they all draw 'in front' of the camera
    //  with the right orientation, that way it wouldn't be necessary to create and dispose fbos for each...

    private void createIconAtlasFromModels(ModelBatch batch, Engine engine) {
        // prep a camera to view whatever is drawn at the origin
        var camera = new PerspectiveCamera(67f, 100, 100);
        camera.near = 0.1f;
        camera.far = 100f;
        camera.position.set(0f, 1f, 1f);
        camera.lookAt(Vector3.Zero);
        camera.update();

        // prep a pixmap packer to pack all the icon textures into an atlas
        var pageWidth = 1024;
        var pageHeight = 1024;
        var pageFormat = Pixmap.Format.RGBA8888;
        var padding = 0;
        var duplicateBorder = false;
        var stripWhitespace = false;
        var packStrategy = new PixmapPacker.GuillotineStrategy();
        var packer = new PixmapPacker(pageWidth, pageHeight, pageFormat,
                padding, duplicateBorder, stripWhitespace, stripWhitespace, packStrategy);

        var models = engine.getSystem(ProviderSystem.class).modelProvider;
        var fbo = new FrameBuffer(Pixmap.Format.RGBA8888, 100, 100, false);
        for (var modelType : MinigolfModels.values()) {
            // get an instance of this model to render to the offscreen buffer
            var key = modelType.key();
            var model = models.getOrCreate(key, assets);
            var instance = new ModelInstance(model);
            instance.transform.rotate(Vector3.Y, 45f);

            // draw it
            fbo.begin();
            {
                ScreenUtils.clear(0f, 0f, 0f, 0f);

                // TODO - it would be nice to have lighting/shading on these too

                batch.begin(camera);
                batch.render(instance);
                //noinspection GDXJavaFlushInsideLoop
                batch.end();

                // extract pixel data from the fbo and pack it into the texture atlas
                var pixmap = Pixmap.createFromFrameBuffer(0, 0, fbo.getWidth(), fbo.getHeight());
                packer.pack(modelType.name(), pixmap);
                pixmap.dispose();
            }
            fbo.end();
        }
        fbo.dispose();

        // NOTE - could also setup PixmapIO to write this atlas out to the filesystem and run this as a preprocessing step
        iconAtlas = packer.generateTextureAtlas(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear, false);
        packer.dispose();
    }

    private void populateStage() {
        var camera = stage.getCamera();

        // --------------------------------
        // settings window and other elements
        {
            var defaultWindowStyle = skin.get("default", Window.WindowStyle.class);
            var glassWindowStyle = new Window.WindowStyle(defaultWindowStyle);
            glassWindowStyle.background = Assets.Patch.metal.drawable;
            glassWindowStyle.titleFont = assets.font;
            glassWindowStyle.titleFontColor = Color.WHITE;

            settings.isShown = false;

            var percentWidth = 0.2f;
            var percentHeight = 1f;
            settings.boundsVisible.set(0f, 0f, percentWidth * camera.viewportWidth, percentHeight * camera.viewportHeight);
            settings.boundsHidden.set(settings.boundsVisible);
            settings.boundsHidden.x -= settings.boundsVisible.width;

            settings.window = new VisWindow("", glassWindowStyle);
            settings.window.pad(10);
//            settings.window.pad(40, 20, 0, 0);
            settings.window.setSize(settings.boundsHidden.width, settings.boundsHidden.height);
            settings.window.setPosition(settings.boundsHidden.x, settings.boundsHidden.y);
            settings.window.align(Align.top | Align.center);
            settings.window.setModal(false);
            settings.window.setMovable(false);
            settings.window.setKeepWithinStage(false);

            var showSettingsDuration = 0.075f;
            var hideSettingsDuration = 0.05f;

            settings.windowShowAction = new MoveToAction();
            settings.windowShowAction.setDuration(showSettingsDuration);
            settings.windowShowAction.setPosition(settings.boundsVisible.x, settings.boundsVisible.y);

            settings.windowHideAction = new MoveToAction();
            settings.windowHideAction.setDuration(hideSettingsDuration);
            settings.windowHideAction.setPosition(settings.boundsHidden.x, settings.boundsHidden.y);

            // populate a vertical scroll pane with buttons corresponding to each available model type
            var scrollTable = new VisTable();
            {
                // radio button style, only 1 checked at a time
                var group = new ButtonGroup<VisImageTextButton>();
                group.setMinCheckCount(0);
                group.setMaxCheckCount(1);

                boolean first = true;
                for (var modelType : MinigolfModels.values()) {
                    var iconRegion = iconAtlas.findRegion(modelType.name());
                    iconRegion.flip(false, true);

                    var iconDrawable = new TextureRegionDrawable(iconRegion);

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

            // --------------------------------
            // show/hide toggle button (arrow handle attached to side of settings window
            var originalButtonStyle = skin.get("default", VisImageButton.VisImageButtonStyle.class);
            var buttonStyle = new VisImageButton.VisImageButtonStyle(originalButtonStyle);
            buttonStyle.imageChecked = new TextureRegionDrawable(assets.atlas.findRegion("icons/arrows-left"));
            buttonStyle.imageUp = new TextureRegionDrawable(assets.atlas.findRegion("icons/arrows-right"));

            var toggleButton = new VisImageButton(buttonStyle);
            toggleButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (settings.isShown) {
                        hideSettings();
                    } else {
                        showSettings();
                    }
                }
            });

            // start 'hidden'
            toggleButton.setSize(35f, 100f);
            toggleButton.setPosition(0, camera.viewportHeight / 2f - toggleButton.getHeight() / 2f);

            settings.buttonShowAction = new MoveToAction();
            settings.buttonShowAction.setDuration(showSettingsDuration);
            settings.buttonShowAction.setPosition(
                    settings.boundsVisible.x + settings.boundsVisible.width,
                    camera.viewportHeight / 2f - toggleButton.getHeight() / 2f);

            settings.buttonHideAction = new MoveToAction();
            settings.buttonHideAction.setDuration(hideSettingsDuration);
            settings.buttonHideAction.setPosition(0, camera.viewportHeight / 2f - toggleButton.getHeight() / 2f);

            settings.toggleButton = toggleButton;

            stage.addActor(settings.toggleButton);
        }

        // --------------------------------
        // level file picker
        {
            var originalStyle = skin.get(VisList.ListStyle.class);
            var style = new List.ListStyle(originalStyle);
            style.background = new NinePatchDrawable(Assets.Patch.glass.drawable);

            levelFileList = new VisList<>(style);
            levelFileList.setWidth(200f);
            updateLevelFilePickerItems();

            var showLevelFilePickerDuration = 0.1f;
            var hideLevelFilePickerDuration = 0.05f;

            showLevelFilePicker = new MoveToAction();
            showLevelFilePicker.setDuration(showLevelFilePickerDuration);
            showLevelFilePicker.setPosition(
                    camera.viewportWidth / 2f - levelFileList.getWidth() / 2f,
                    camera.viewportHeight / 2f - levelFileList.getHeight() / 2f);

            hideLevelFilePicker = new MoveToAction();
            hideLevelFilePicker.setDuration(hideLevelFilePickerDuration);
            hideLevelFilePicker.setPosition(
                    camera.viewportWidth / 2f - levelFileList.getWidth() / 2f,
                    -levelFileList.getHeight());

            // start hidden
            levelFileList.setPosition(
                    camera.viewportWidth / 2f - levelFileList.getWidth() / 2f,
                    -levelFileList.getHeight());

            stage.addActor(levelFileList);
        }

        // --------------------------------
        // top level buttons
        // TODO - add a text field for the current level name
        {
            var width = 70;
            var height = 50;

            var loadLevelButton = new VisTextButton("Load");
            loadLevelButton.setSize(width, height);
            loadLevelButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    // TODO - trigger this when clicking on a filename to load from the file picker ui
                    loadLevelData("test.json");
                    showLevelFilePicker.reset();
                    levelFileList.addAction(showLevelFilePicker);
                }
            });

            var saveLevelButton = new VisTextButton("Save");
            saveLevelButton.setSize(width, height);
            saveLevelButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    saveLevelData("test.json");
                    hideLevelFilePicker.reset();
                    levelFileList.addAction(hideLevelFilePicker);
                }
            });

            var originalStyle = skin.get("default", VisWindow.WindowStyle.class);
            var style = new VisWindow.WindowStyle(originalStyle);
            style.background = Assets.Patch.metal.drawable;
            style.titleFont = assets.smallFont;
            style.titleFontColor = Color.WHITE;

            var levelsWindow = new VisWindow("Levels", style);
            levelsWindow.setSize(width, 2 * height);
            levelsWindow.setPosition(camera.viewportWidth - width, 0);

            levelsWindow.defaults().padTop(5f);
            levelsWindow.add(loadLevelButton).growX().row();
            levelsWindow.add(saveLevelButton).growX();

            stage.addActor(levelsWindow);
        }
    }

    private void saveLevelData(String filename) {
        var tileEntities = engine.getEntitiesFor(ComponentFamilies.tiles);
        if (tileEntities.size() > 0) {
            var tileInfos = new Array<TileInfo>();
            for (var entity : tileEntities) {
                var tile = ComponentMappers.tiles.get(entity);
                var tileInfo = new TileInfo(tile);
                tileInfos.add(tileInfo);
            }
            var levelInfo = new LevelFileInfo(tileInfos);
            var json = new Json(JsonWriter.OutputType.json);
            var jsonData = json.prettyPrint(levelInfo);//, LevelFileInfo.class)
            var path = "levels/" + filename;
            var file = Gdx.files.getFileHandle(path, Files.FileType.Local);
            file.writeString(jsonData, false, StandardCharsets.UTF_8.name());
            Gdx.app.log(TAG, "wrote level data to " + filename);
        } else {
            Gdx.app.log(TAG, "no tile data to write level");
        }
        // TODO - pop a toast with results (saved / failed)
    }

    private void loadLevelData(String filename) {
        var path = "levels/" + filename;
        var file = Gdx.files.local(path);
        if (file.exists()) {
            var json = new Json();
            var jsonData = file.readString(StandardCharsets.UTF_8.name());
            var levelData = json.fromJson(LevelFileInfo.class, jsonData);
            if (!levelData.tileInfos.isEmpty()) {
                // clear existing tiles before loading a new one
                engine.removeAllEntities(ComponentFamilies.tiles);

                for (var tileInfo : levelData.tileInfos) {
                    var modelType = MinigolfModels.valueOf(tileInfo.modelType);
                    EntityFactory.createTile(modelType, engine, assets, tileInfo.x, tileInfo.z, tileInfo.yRotation);
                }
            }
        }
    }

    private void updateLevelFilePickerItems() {
        var levelFileLabels = new Array<String>();
        var levelsDir = Gdx.files.getFileHandle("levels", Files.FileType.Internal);
        for (var file : levelsDir.list("json")) {
            var filename = file.name();
            var name = filename.substring(0, filename.lastIndexOf(".json"));
            levelFileLabels.add(name);
        }
        if (levelFileLabels.isEmpty()) {
            levelFileLabels.add("[no files available]");
        }
        levelFileList.setItems(levelFileLabels);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TileInfo {
        int x;
        int z;
        float yRotation;
        String modelType;
        public TileInfo(TileComponent tile) {
            this(tile.xCoord, tile.zCoord, tile.yRotation, tile.modelType.name());
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelFileInfo {
        Array<TileInfo> tileInfos;
    }

}
