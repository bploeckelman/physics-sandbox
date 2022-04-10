package zendo.games.physics;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.utils.Disposable;

/**
 * A 3d object in the game.
 * Only instantiate through GameObject.Builder.
 * Manages it's own Bullet collision object and therefore must be disposed when no longer in use.
 */
public class GameObject extends ModelInstance implements Disposable {

    public enum Type {
        GROUND, SPHERE, BOX, CONE, CAPSULE, CYLINDER;
        private static final Type[] values = Type.values();
        private static final int numTypes = Type.values().length;
        public static Type random() {
            // NOTE - skips GROUND type
            return values[MathUtils.random(1, numTypes - 1)];
        }
    }

    public final Model model;
    public final btCollisionShape collisionShape;
    public final btCollisionObject collisionObject;

    public boolean moving = false;

    private GameObject(Model model, String nodeId, btCollisionShape collisionShape) {
        super(model, nodeId);
        this.model = model;
        this.collisionShape = collisionShape;
        this.collisionObject = new btCollisionObject();
        this.collisionObject.setCollisionShape(collisionShape);
    }

    @Override
    public void dispose() {
        collisionObject.dispose();
    }

    // ------------------------------------------------------------------------

    /**
     * Convenience class for building GameObjects simply.
     * Holds ownership over it's btCollisionShape and must dispose of it when it's no longer used.
     * @param model the parent Model that this GameObject is built from
     * @param nodeId the Node identifier string for which MeshPart of the parent Model to create an instance from
     * @param collisionShape the Bullet collision shape to use for the constructed GameObject
     */
    public record Builder(Model model, String nodeId, btCollisionShape collisionShape) implements Disposable {
        public GameObject build() {
            return new GameObject(model, nodeId, collisionShape);
        }
        @Override
        public void dispose() {
            collisionShape.dispose();
        }
    }

}
