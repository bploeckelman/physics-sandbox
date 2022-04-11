package zendo.games.physics;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
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

    private static class MotionState extends btMotionState {
        private final Matrix4 transform;

        public MotionState(Matrix4 transform) {
            this.transform = transform;
        }

        // called by Bullet when it needs to know the current transform of the object
        // eg. when it's added to the world
        @Override
        public void getWorldTransform(Matrix4 worldTrans) {
            worldTrans.set(transform);
        }

        // called by Bullet when it has transformed a dynamic object
        @Override
        public void setWorldTransform(Matrix4 worldTrans) {
            transform.set(worldTrans);
            // TODO - if object would fall out of the world (y pos < threshold), remove it from the world
        }
    }

    public final Model model;
    public final btRigidBody rigidBody;
    public final MotionState motionState;

    private GameObject(Model model, String nodeId, btRigidBody.btRigidBodyConstructionInfo constructionInfo) {
        super(model, nodeId);
        this.model = model;
        this.motionState = new MotionState(transform);
        this.rigidBody = new btRigidBody(constructionInfo);
        this.rigidBody.setMotionState(motionState);

        // add a blending attribute so alpha in the diffuse attribute is respected
        this.materials.first().set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
    }

    @Override
    public void dispose() {
        rigidBody.dispose();
        motionState.dispose();
    }

    // ------------------------------------------------------------------------

    /**
     * Convenience class for easily building GameObjects and their rendering and physics components.
     * Holds ownership over it's collision shape and rigid body construction info
     * and therefore it must be disposed of when it's no longer used.
     */
    public static class Builder implements Disposable {
        private static final Vector3 localInertia = new Vector3();

        private final Model model;
        private final String nodeId;
        private final btCollisionShape collisionShape;
        private final btRigidBody.btRigidBodyConstructionInfo constructionInfo;

        /**
         * @param mass the mass of the constructed GameObject's physics component
         * @param model the parent Model that this GameObject is built from
         * @param nodeId the Node identifier string for which MeshPart of the parent Model to create an instance from
         * @param collisionShape the Bullet collision shape to use for the constructed GameObject
         */
        public Builder(float mass, Model model, String nodeId, btCollisionShape collisionShape) {
            this.model = model;
            this.nodeId = nodeId;
            this.collisionShape = collisionShape;
            Builder.localInertia.setZero();
            if (mass > 0f) {
                collisionShape.calculateLocalInertia(mass, Builder.localInertia);
            }
            this.constructionInfo = new btRigidBody.btRigidBodyConstructionInfo(mass, null, collisionShape, Builder.localInertia);
        }

        /**
         * @return a new GameObject with the parameters from this Builder
         */
        public GameObject build() {
            return new GameObject(model, nodeId, constructionInfo);
        }

        @Override
        public void dispose() {
            collisionShape.dispose();
            constructionInfo.dispose();
        }
    }

}
