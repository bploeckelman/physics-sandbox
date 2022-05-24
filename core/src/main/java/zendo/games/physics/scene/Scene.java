package zendo.games.physics.scene;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import zendo.games.physics.Game;
import zendo.games.physics.scene.components.ModelInstanceComponent;
import zendo.games.physics.scene.components.NameComponent;
import zendo.games.physics.scene.components.PhysicsComponent;
import zendo.games.physics.scene.providers.ModelProvider;
import zendo.games.physics.scene.systems.ProviderSystem;

import static zendo.games.physics.scene.providers.CollisionShapeProvider.Type;

public class Scene implements Disposable {

    private final Engine engine;
    private final Environment environment;

    private int numCratesSpawned = 1;

    public DirectionalShadowLight shadowLight;

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

        buildTestEntities();
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

    private void buildTestEntities() {
        String name;
        Entity entity;
        var providers = engine.getSystem(ProviderSystem.class);

        name = "physics_floor";
        {
            var size = 80f;
            var collisionShape = providers.collisionShapeProvider
                    .builder(Type.rect, name)
                    .halfExtents(size / 2, 0f, size / 2)
                    .build();

            var model = providers.modelProvider.get(ModelProvider.Node.patch);
            var modelInstanceComponent = new ModelInstanceComponent(model, ModelProvider.Node.patch.name());

            var transform = modelInstanceComponent.transform.cpy();
            modelInstanceComponent.transform.setToScaling(size, 1f, size);

            entity = engine.createEntity()
                    .add(new NameComponent(name))
                    .add(new PhysicsComponent(0, transform, collisionShape))
                    .add(modelInstanceComponent);
            engine.addEntity(entity);
        }

        name = "axes";
        {
            entity = engine.createEntity()
                    .add(new NameComponent(name))
                    .add(providers.modelProvider.createModelInstanceComponent(ModelProvider.Node.axes));
            engine.addEntity(entity);
        }
    }

    public void spawnCrate() {
        var providers = engine.getSystem(ProviderSystem.class);

        var collisionShape = providers.collisionShapeProvider.get(Type.box);

        var node = ModelProvider.Node.cube;
        var modelInstanceComponent = providers.modelProvider.createModelInstanceComponent(node);

        // modify initial transform
        var transform = modelInstanceComponent.transform;
        transform.translate(0, 15, 0);

        // modify material texture
        var material = modelInstanceComponent.getMaterial(node.name());
        material.get(TextureAttribute.class, TextureAttribute.Diffuse)
                .textureDescription.texture = Game.instance.assets.crateTexture;

        var entity = engine.createEntity()
                .add(new NameComponent("crate " + numCratesSpawned++))
                .add (new PhysicsComponent(transform, collisionShape))
                .add(modelInstanceComponent);
        engine.addEntity(entity);
    }

}
