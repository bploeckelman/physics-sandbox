package zendo.games.physics.scene;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Family;

public class Components {

    public record Name(String name) implements Component {}

    public static final ComponentMapper<Name> name = ComponentMapper.getFor(Name.class);

    public static final Family names = Family.all(Components.Name.class).get();

}
