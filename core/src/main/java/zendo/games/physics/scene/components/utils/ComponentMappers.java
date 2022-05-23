package zendo.games.physics.scene.components.utils;

import com.badlogic.ashley.core.ComponentMapper;
import zendo.games.physics.scene.components.ModelInstanceComponent;
import zendo.games.physics.scene.components.NameComponent;
import zendo.games.physics.scene.components.PhysicsComponent;

public class ComponentMappers {
    public static final ComponentMapper<ModelInstanceComponent> modelInstance = ComponentMapper.getFor(ModelInstanceComponent.class);
    public static final ComponentMapper<NameComponent>          name          = ComponentMapper.getFor(NameComponent.class);
    public static final ComponentMapper<PhysicsComponent>       physics       = ComponentMapper.getFor(PhysicsComponent.class);
}
