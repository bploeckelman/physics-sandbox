package zendo.games.physics.scene;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;

public class Scene {

    private final Engine engine;

    public Scene(Engine engine) {
        this.engine = engine;

        var entity = new Entity().add(new Components.Name("Test"));
        this.engine.addEntity(entity);
    }

    public void update(float delta) {

    }

}
