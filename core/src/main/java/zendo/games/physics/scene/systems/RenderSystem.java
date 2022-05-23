package zendo.games.physics.scene.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ObjectSet;
import zendo.games.physics.scene.components.ModelInstanceComponent;
import zendo.games.physics.scene.components.utils.ComponentMappers;

public class RenderSystem extends EntitySystem implements EntityListener {

    private final ObjectSet<Entity> entities = new ObjectSet<>();
    private final ObjectSet<ModelInstanceComponent> components = new ObjectSet<>();
    private final ComponentMapper<ModelInstanceComponent> mapper = ComponentMappers.modelInstance;

    @Override
    public void entityAdded(Entity entity) {
        var component = mapper.get(entity);
        components.add(component);
        entities.add(entity);
    }

    @Override
    public void entityRemoved(Entity entity) {
        var component = mapper.get(entity);
        components.remove(component);
        entities.remove(entity);
    }

    public void render(Camera camera, ModelBatch batch, Environment environment) {
        batch.begin(camera);
        batch.render(components, environment);
        batch.end();
    }

    public void renderShadows(Camera camera, ModelBatch shadowModelBatch, DirectionalShadowLight shadowLight) {
        shadowLight.begin(Vector3.Zero, camera.direction);
        shadowModelBatch.begin(shadowLight.getCamera());
        shadowModelBatch.render(components);
        shadowModelBatch.end();
        shadowLight.end();
    }

}
