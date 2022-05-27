package zendo.games.physics.scene;

import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import zendo.games.physics.scene.factories.EntityFactory;

public class Scene implements Disposable {

    private final Engine engine;
    private final Environment environment;

    public final DirectionalShadowLight shadowLight;

    public Scene(Engine engine) {
        this.engine = engine;

        this.environment = new Environment();
        this.environment.set(ColorAttribute.createAmbientLight(0.3f, 0.3f, 0.3f, 1f));

        var sunlight = new Color(244f / 255f, 233f / 255f, 155f / 255f, 1f);
        var lightDir = new Vector3(-1f, -0.8f, -0.2f);
        this.shadowLight = new DirectionalShadowLight(
                4096, 4096,
                100f, 100f,
                0.1f, 1000f
        );
        shadowLight.set(sunlight, lightDir);
        environment.add(shadowLight);
        environment.shadowMap = shadowLight;

        createInitialEntities();
    }

    @Override
    public void dispose() {
        shadowLight.dispose();
    }

    public Environment env() {
        return environment;
    }

    public void update(float delta) {
        // ...
    }

    private void createInitialEntities() {
        EntityFactory.createFloor(engine);
        EntityFactory.createOriginAxes(engine);
    }

}
