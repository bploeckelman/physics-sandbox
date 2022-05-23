package zendo.games.physics.scene.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.utils.Array;
import zendo.games.physics.scene.Components;
import zendo.games.physics.scene.components.ModelInstanceComponent;

public class RenderSystem extends EntitySystem implements EntityListener {

    private final Array<Entity> entities = new Array<>();
    private final Array<ModelInstanceComponent> components = new Array<>();
    private final ComponentMapper<ModelInstanceComponent> mapper = Components.Mappers.modelInstanceComponents;

    @Override
    public void entityAdded(Entity entity) {
        var component = mapper.get(entity);
        var hasComponent = component != null;
        var notAlreadyTracked = !entities.contains(entity, true);
        if (hasComponent && notAlreadyTracked) {
            entities.add(entity);
            components.add(component);
        }
    }

    @Override
    public void entityRemoved(Entity entity) {
        var component = mapper.get(entity);
        components.removeValue(component, true);
        entities.removeValue(entity, true);
    }

    public void render(ModelBatch batch) {
        batch.render(components);
    }

    public void render(ModelBatch batch, Environment environment) {
        batch.render(components, environment);
    }

}
