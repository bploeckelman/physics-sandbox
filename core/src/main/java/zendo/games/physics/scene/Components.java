package zendo.games.physics.scene;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Family;
import zendo.games.physics.scene.components.NameComponent;
import zendo.games.physics.scene.components.ModelInstanceComponent;

public class Components {

    public static class Mappers {
        public static final ComponentMapper<NameComponent> name = ComponentMapper.getFor(NameComponent.class);
        public static final ComponentMapper<ModelInstanceComponent> modelInstance = ComponentMapper.getFor(ModelInstanceComponent.class);
    }

    public static class Families {
        public static final Family name = Family.all(NameComponent.class).get();
        public static final Family modelInstance = Family.all(ModelInstanceComponent.class).get();
    }

}
