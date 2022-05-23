package zendo.games.physics.scene.systems;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.utils.Disposable;
import zendo.games.physics.scene.providers.CollisionShapeProvider;
import zendo.games.physics.scene.providers.ModelProvider;

public class ProviderSystem extends EntitySystem implements Disposable {

    public final ModelProvider modelProvider;
    public final CollisionShapeProvider collisionShapeProvider;

    public ProviderSystem() {
        this.modelProvider = new ModelProvider();
        this.collisionShapeProvider = new CollisionShapeProvider();
    }

    @Override
    public void dispose() {
        modelProvider.dispose();
        collisionShapeProvider.dispose();
    }

}
