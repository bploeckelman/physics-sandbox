package zendo.games.physics.scene.components.utils;

import com.badlogic.ashley.core.Family;
import zendo.games.physics.scene.components.*;

public class ComponentFamilies {
    public static final Family modelInstances = Family.all(ModelInstanceComponent.class).get();
    public static final Family names          = Family.all(NameComponent.class).get();
    public static final Family physics        = Family.all(PhysicsComponent.class).get();
    public static final Family coord2         = Family.all(Coord2Component.class).get();
    public static final Family tiles          = Family.all(TileComponent.class).get();
}
