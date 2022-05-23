package zendo.games.physics.scene.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.linearmath.LinearMath;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectSet;
import zendo.games.physics.scene.components.PhysicsComponent;
import zendo.games.physics.scene.components.utils.ComponentMappers;

public class PhysicsSystem extends EntitySystem implements EntityListener, Disposable {

    private final ObjectSet<Entity> entities = new ObjectSet<>();
    private final ObjectSet<PhysicsComponent> components = new ObjectSet<>();
    private final ComponentMapper<PhysicsComponent> mapper = ComponentMappers.physics;


    public PhysicsSystem() {
        Bullet.init();
        Gdx.app.log(Bullet.class.getSimpleName(), "version " + LinearMath.btGetVersion());

    }

    @Override
    public void dispose() {
        components.forEach(PhysicsComponent::dispose);
    }

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

    @Override
    public void update(float delta) {
//        for (var component : components) {
//            // TODO - update physics held in components
//        }
    }

}