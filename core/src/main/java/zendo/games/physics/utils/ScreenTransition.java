package zendo.games.physics.utils;

import aurelienribon.tweenengine.primitives.MutableFloat;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import zendo.games.physics.Assets;
import zendo.games.physics.Config;
import zendo.games.physics.screens.BaseScreen;

public class ScreenTransition implements Disposable {

    public boolean inProgress;
    public MutableFloat percent;
    public ShaderProgram shader;

    static class FrameBuffers {
        public FrameBuffer source;
        public FrameBuffer dest;
    }
    public FrameBuffers frameBuffers;

    static class Textures {
        public Texture source;
        public Texture dest;
    }
    public Textures textures;

    public ScreenTransition(Assets assets) {
        if (!assets.initialized) {
            throw new GdxRuntimeException("Assets must be initialized to create screen transitions");
        }

        this.inProgress = false;
        this.percent = new MutableFloat(0);
        this.shader = assets.transitionShaders.get(Assets.Transition.dreamy);

        this.frameBuffers = new FrameBuffers();
        this.frameBuffers.source = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        this.frameBuffers.dest = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        this.textures = new Textures();
        this.textures.source = frameBuffers.source.getColorBufferTexture();
        this.textures.dest = frameBuffers.dest.getColorBufferTexture();
    }

    public void set(Assets assets, Assets.Transition transition) {
        shader = assets.transitionShaders.get(transition);
    }

    public void render(SpriteBatch batch, BaseScreen currentScreen, BaseScreen nextScreen, Camera camera) {
        nextScreen.update(Time.delta);
        nextScreen.renderIntoFrameBuffers();

        // draw next screen to dest framebuffer
        frameBuffers.dest.begin();
        {
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            nextScreen.render();
        }
        frameBuffers.dest.end();

        // draw current screen to source framebuffer
        frameBuffers.source.begin();
        {
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            currentScreen.render();
        }
        frameBuffers.source.end();

        // draw composited framebuffer to window
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        batch.setShader(shader);
        batch.begin();
        {
            textures.source.bind(1);
            shader.setUniformi("u_texture1", 1);

            textures.dest.bind(0);
            shader.setUniformi("u_texture", 0);

            shader.setUniformf("u_percent", percent.floatValue());

            batch.setColor(Color.WHITE);
            batch.draw(textures.dest, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
        batch.end();
        batch.setShader(null);

        if (Config.Debug.general) {
            batch.begin();
            {
                batch.draw(textures.dest, 0, 100, 100, -100);
                batch.draw(textures.source, 100, 100, 100, -100);
            }
            batch.end();
        }
    }

    @Override
    public void dispose() {
        if (frameBuffers.source != null) {
            frameBuffers.source.dispose();
        }
        if (frameBuffers.dest != null) {
            frameBuffers.dest.dispose();
        }
        if (textures.source != null) {
            textures.source.dispose();
        }
        if (textures.dest != null) {
            textures.dest.dispose();
        }
    }

}
