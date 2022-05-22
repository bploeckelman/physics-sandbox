package zendo.games.physics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import space.earlygrey.shapedrawer.ShapeDrawer;

public class Assets implements Disposable {

    public enum Load { ASYNC, SYNC }

    public boolean initialized;

    public SpriteBatch batch;
    public ModelBatch modelBatch;
    public ModelBatch debugModelBatch;
    public ModelBatch shadowModelBatch;
    public ShapeDrawer shapes;
    public GlyphLayout layout;
    public AssetManager mgr;
    public TextureAtlas atlas;

    public BitmapFont font;
    public BitmapFont smallFont;
    public BitmapFont largeFont;

    public Texture pixel;
    public Texture libgdxTexture;
    public Texture prototypeGridOrange;
    public TextureRegion pixelRegion;

    public enum Transition {
        blinds, circle, crosshatch, cube, dissolve, doom, doorway,
        dreamy, heart, pixelize, radial, ripple, starwars, stereo
    }
    public ObjectMap<Transition, ShaderProgram> transitionShaders;

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
        debugModelBatch = new ModelBatch();
        shadowModelBatch = new ModelBatch();
        shapes = new ShapeDrawer(batch, pixelRegion);
        layout = new GlyphLayout();

        mgr = new AssetManager();
        {
//            mgr.load("sprites/sprites.atlas", TextureAtlas.class);

            mgr.load("libgdx.png", Texture.class);

            var param = new TextureLoader.TextureParameter();
            param.minFilter = Texture.TextureFilter.MipMapLinearLinear;
            param.magFilter = Texture.TextureFilter.MipMapLinearLinear;
            param.wrapU = Texture.TextureWrap.Repeat;
            param.wrapV = Texture.TextureWrap.Repeat;
            param.genMipMaps = true;
            mgr.load("prototype-grid-orange.png", Texture.class, param);
        }

        if (load == Load.SYNC) {
            mgr.finishLoading();
            updateLoading();
        }
    }

    public float updateLoading() {
        if (!mgr.update()) return mgr.getProgress();
        if (initialized) return 1;

//        atlas = mgr.get("sprites/sprites.atlas");

        libgdxTexture = mgr.get("libgdx.png", Texture.class);
        prototypeGridOrange = mgr.get("prototype-grid-orange.png", Texture.class);

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
        modelBatch.dispose();
        debugModelBatch.dispose();
        shadowModelBatch.dispose();
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