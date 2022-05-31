package zendo.games.physics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import space.earlygrey.shapedrawer.ShapeDrawer;
import zendo.games.physics.shaders.WireframeShader;

public class Assets implements Disposable {

    public enum Load { ASYNC, SYNC }

    public boolean initialized;

    public SpriteBatch batch;
    public ModelBatch modelBatch;
    public ModelBatch wireframeModelBatch;
    public ModelBatch shadowModelBatch;
    public ShapeRenderer shapeRenderer;
    public ShapeDrawer shapes;
    public GlyphLayout layout;
    public AssetManager mgr;
    public TextureAtlas atlas;

    public BitmapFont font;
    public BitmapFont smallFont;
    public BitmapFont largeFont;

    public Texture pixel;
    public Texture libgdxTexture;
    public Texture metalTexture;
    public Texture crateTexture;
    public Texture prototypeGridOrange;
    public TextureRegion pixelRegion;

    public enum Transition {
        blinds, circle, crosshatch, cube, dissolve, doom, doorway,
        dreamy, heart, pixelize, radial, ripple, starwars, stereo
    }
    public ObjectMap<Transition, ShaderProgram> transitionShaders;

    public enum Patch {
        debug, panel, metal, glass, glass_green, glass_yellow, glass_dim, glass_active;
        public NinePatch ninePatch;
        public NinePatchDrawable drawable;
    }

    public Assets() {
        this(Load.SYNC);
    }

    public Assets(Load load) {
        initialized = false;

        // create a single pixel texture and associated region
        var pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        {
            pixmap.setColor(Color.WHITE);
            pixmap.drawPixel(0, 0);
            pixel = new Texture(pixmap);
        }
        pixmap.dispose();
        pixelRegion = new TextureRegion(pixel);

        // generate fonts
        {
            final int baseSize = 20;

            var fontFile = Gdx.files.internal("fonts/outfit-medium.ttf");
            var generator = new FreeTypeFontGenerator(fontFile);
            var parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = baseSize;
            parameter.color = Color.WHITE;
            parameter.borderColor = Color.DARK_GRAY;
            parameter.shadowColor = Color.BLACK;
            parameter.borderWidth = 2;
            parameter.shadowOffsetX = 1;
            parameter.shadowOffsetY = 2;
            font = generator.generateFont(parameter);

            parameter.size = baseSize / 2;
            smallFont = generator.generateFont(parameter);

            parameter.size = 2 * baseSize;
            largeFont = generator.generateFont(parameter);

            generator.dispose();
        }

        batch = new SpriteBatch();
        modelBatch = new ModelBatch();
        shadowModelBatch = new ModelBatch(new DepthShaderProvider());
        wireframeModelBatch = new ModelBatch(new DefaultShaderProvider() {
            @Override
            protected Shader createShader(Renderable renderable) {
                return new WireframeShader(renderable, config);
            }
        });

        shapeRenderer = new ShapeRenderer();
        shapes = new ShapeDrawer(batch, pixelRegion);
        layout = new GlyphLayout();

        mgr = new AssetManager();
        {
            mgr.load("sprites/sprites.atlas", TextureAtlas.class);
            mgr.load("gui/uiskin.json", Skin.class);

            // textures ---------------------------------------------
            var param = new TextureLoader.TextureParameter();
            param.minFilter = Texture.TextureFilter.MipMapLinearLinear;
            param.magFilter = Texture.TextureFilter.MipMapLinearLinear;
            param.wrapU = Texture.TextureWrap.Repeat;
            param.wrapV = Texture.TextureWrap.Repeat;
            param.genMipMaps = true;

            mgr.load("libgdx.png", Texture.class);
            mgr.load("prototype-grid-orange-lighter.png", Texture.class, param);
            mgr.load("crate.png", Texture.class, param);
            mgr.load("metal.png", Texture.class, param);

            // models -----------------------------------------------
            mgr.load("minigolf/block.g3dj", Model.class);
            mgr.load("minigolf/bump-up.g3dj", Model.class);
            mgr.load("minigolf/bump-up-walls.g3dj", Model.class);
            mgr.load("minigolf/bump-down.g3dj", Model.class);
            mgr.load("minigolf/bump-down-walls.g3dj", Model.class);
            mgr.load("minigolf/castle.g3dj", Model.class);
            mgr.load("minigolf/corner.g3dj", Model.class);
            mgr.load("minigolf/corner-inner.g3dj", Model.class);
            mgr.load("minigolf/corner-square-a.g3dj", Model.class);
            mgr.load("minigolf/crest.g3dj", Model.class);
            mgr.load("minigolf/end.g3dj", Model.class);
            mgr.load("minigolf/gap.g3dj", Model.class);
            mgr.load("minigolf/hill-corner.g3dj", Model.class);
            mgr.load("minigolf/hill-round.g3dj", Model.class);
            mgr.load("minigolf/hill-square.g3dj", Model.class);
            mgr.load("minigolf/hole-open.g3dj", Model.class);
            mgr.load("minigolf/hole-round.g3dj", Model.class);
            mgr.load("minigolf/hole-square.g3dj", Model.class);
            mgr.load("minigolf/narrow-block.g3dj", Model.class);
            mgr.load("minigolf/narrow-round.g3dj", Model.class);
            mgr.load("minigolf/narrow-square.g3dj", Model.class);
            mgr.load("minigolf/obstacle-block.g3dj", Model.class);
            mgr.load("minigolf/obstacle-diamond.g3dj", Model.class);
            mgr.load("minigolf/obstacle-triangle.g3dj", Model.class);
            mgr.load("minigolf/open.g3dj", Model.class);
            mgr.load("minigolf/ramp-a.g3dj", Model.class);
            mgr.load("minigolf/ramp-b.g3dj", Model.class);
            mgr.load("minigolf/ramp-c.g3dj", Model.class);
            mgr.load("minigolf/ramp-d.g3dj", Model.class);
            mgr.load("minigolf/ramp-sharp.g3dj", Model.class);
            mgr.load("minigolf/ramp-square.g3dj", Model.class);
            mgr.load("minigolf/round-corner-a.g3dj", Model.class);
            mgr.load("minigolf/round-corner-b.g3dj", Model.class);
            mgr.load("minigolf/round-corner-c.g3dj", Model.class);
            mgr.load("minigolf/side.g3dj", Model.class);
            mgr.load("minigolf/split.g3dj", Model.class);
            mgr.load("minigolf/split-t.g3dj", Model.class);
            mgr.load("minigolf/split-walls-to-open.g3dj", Model.class);
            mgr.load("minigolf/start.g3dj", Model.class);
            mgr.load("minigolf/straight.g3dj", Model.class);
            mgr.load("minigolf/tunnel-double.g3dj", Model.class);
            mgr.load("minigolf/tunnel-narrow.g3dj", Model.class);
            mgr.load("minigolf/tunnel-wide.g3dj", Model.class);
            mgr.load("minigolf/wall-left.g3dj", Model.class);
            mgr.load("minigolf/wall-right.g3dj", Model.class);
            mgr.load("minigolf/walls-to-open.g3dj", Model.class);
            mgr.load("minigolf/windmill.g3dj", Model.class);
        }

        if (load == Load.SYNC) {
            mgr.finishLoading();
            updateLoading();
        }
    }

    public float updateLoading() {
        if (!mgr.update()) return mgr.getProgress();
        if (initialized) return 1;

        atlas = mgr.get("sprites/sprites.atlas");

        libgdxTexture = mgr.get("libgdx.png", Texture.class);
        metalTexture = mgr.get("metal.png", Texture.class);
        crateTexture = mgr.get("crate.png", Texture.class);
        prototypeGridOrange = mgr.get("prototype-grid-orange-lighter.png", Texture.class);

        // initialize patch values
        Patch.debug.ninePatch        = new NinePatch(atlas.findRegion("ninepatch/debug"), 2, 2, 2, 2);
        Patch.panel.ninePatch        = new NinePatch(atlas.findRegion("ninepatch/panel"), 15, 15, 15, 15);
        Patch.glass.ninePatch        = new NinePatch(atlas.findRegion("ninepatch/glass"), 8, 8, 8, 8);
        Patch.glass_green.ninePatch  = new NinePatch(atlas.findRegion("ninepatch/glass-green"), 8, 8, 8, 8);
        Patch.glass_yellow.ninePatch = new NinePatch(atlas.findRegion("ninepatch/glass-yellow"), 8, 8, 8, 8);
        Patch.glass_dim.ninePatch    = new NinePatch(atlas.findRegion("ninepatch/glass-dim"), 8, 8, 8, 8);
        Patch.glass_active.ninePatch = new NinePatch(atlas.findRegion("ninepatch/glass-active"), 8, 8, 8, 8);
        Patch.metal.ninePatch        = new NinePatch(atlas.findRegion("ninepatch/metal"), 12, 12, 12, 12);

        Patch.debug.drawable        = new NinePatchDrawable(Patch.debug.ninePatch);
        Patch.panel.drawable        = new NinePatchDrawable(Patch.panel.ninePatch);
        Patch.glass.drawable        = new NinePatchDrawable(Patch.glass.ninePatch);
        Patch.glass_green.drawable  = new NinePatchDrawable(Patch.glass_green.ninePatch);
        Patch.glass_yellow.drawable = new NinePatchDrawable(Patch.glass_yellow.ninePatch);
        Patch.glass_dim.drawable    = new NinePatchDrawable(Patch.glass_dim.ninePatch);
        Patch.glass_active.drawable = new NinePatchDrawable(Patch.glass_active.ninePatch);
        Patch.metal.drawable        = new NinePatchDrawable(Patch.metal.ninePatch);

        String defaultVertexPath = "shaders/default.vert";
        {
            transitionShaders = new ObjectMap<>();
            transitionShaders.put(Transition.blinds,     loadShader(defaultVertexPath, "shaders/transitions/blinds.frag"));
            transitionShaders.put(Transition.circle,     loadShader(defaultVertexPath, "shaders/transitions/circlecrop.frag"));
            transitionShaders.put(Transition.crosshatch, loadShader(defaultVertexPath, "shaders/transitions/crosshatch.frag"));
            transitionShaders.put(Transition.cube,       loadShader(defaultVertexPath, "shaders/transitions/cube.frag"));
            transitionShaders.put(Transition.dissolve,   loadShader(defaultVertexPath, "shaders/transitions/dissolve.frag"));
            transitionShaders.put(Transition.doom,       loadShader(defaultVertexPath, "shaders/transitions/doomdrip.frag"));
            transitionShaders.put(Transition.doorway,    loadShader(defaultVertexPath, "shaders/transitions/doorway.frag"));
            transitionShaders.put(Transition.dreamy,     loadShader(defaultVertexPath, "shaders/transitions/dreamy.frag"));
            transitionShaders.put(Transition.heart,      loadShader(defaultVertexPath, "shaders/transitions/heart.frag"));
            transitionShaders.put(Transition.pixelize,   loadShader(defaultVertexPath, "shaders/transitions/pixelize.frag"));
            transitionShaders.put(Transition.radial,     loadShader(defaultVertexPath, "shaders/transitions/radial.frag"));
            transitionShaders.put(Transition.ripple,     loadShader(defaultVertexPath, "shaders/transitions/ripple.frag"));
            transitionShaders.put(Transition.starwars,   loadShader(defaultVertexPath, "shaders/transitions/starwars.frag"));
            transitionShaders.put(Transition.stereo,     loadShader(defaultVertexPath, "shaders/transitions/stereo.frag"));
        }

        initialized = true;
        return 1;
    }

    @Override
    public void dispose() {
        mgr.dispose();
        batch.dispose();
        shapeRenderer.dispose();
        modelBatch.dispose();
        shadowModelBatch.dispose();
        wireframeModelBatch.dispose();
        pixel.dispose();
        font.dispose();
        smallFont.dispose();
        largeFont.dispose();
        transitionShaders.values().forEach(ShaderProgram::dispose);
    }

    public static ShaderProgram loadShader(String vertSourcePath, String fragSourcePath) {
        ShaderProgram.pedantic = false;
        ShaderProgram shaderProgram = new ShaderProgram(
                Gdx.files.internal(vertSourcePath),
                Gdx.files.internal(fragSourcePath));
        String log = shaderProgram.getLog();

        if (!shaderProgram.isCompiled()) {
            Gdx.app.error("LoadShader", "compilation failed:\n" + log);
            throw new GdxRuntimeException("LoadShader: compilation failed:\n" + log);
        } else if (Config.Debug.shaders){
            Gdx.app.debug("LoadShader", "ShaderProgram compilation log: " + log);
        }

        return shaderProgram;
    }

}
