package zendo.games.physics.scene.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
import com.badlogic.gdx.utils.Disposable;
import lombok.RequiredArgsConstructor;
import zendo.games.physics.Game;
import zendo.games.physics.scene.components.utils.ComponentFamilies;
import zendo.games.physics.scene.systems.PhysicsSystem;

import static com.badlogic.gdx.physics.bullet.collision.btCollisionObject.CollisionFlags;
import static com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;

public class PhysicsComponent implements Component, Disposable {

    private static final Vector3 localInertia = new Vector3();

    private final MotionState motionState;
    private final btCollisionShape collisionShape;
    private final btRigidBodyConstructionInfo constructionInfo;

    public final btRigidBody rigidBody;
    public float mass;
    public boolean outOfBounds;

    public PhysicsComponent(Matrix4 transform, btCollisionShape collisionShape) {
        this(1f, transform, collisionShape);
    }

    public PhysicsComponent(float mass, Matrix4 transform, btCollisionShape collisionShape) {
        this.mass = mass;
        this.motionState = new MotionState(transform);
        this.collisionShape = collisionShape;

        localInertia.setZero();
        if (mass > 0) {
            collisionShape.calculateLocalInertia(mass, localInertia);
        }

        this.constructionInfo = new btRigidBodyConstructionInfo(mass, motionState, collisionShape, localInertia.cpy());
        this.rigidBody = new btRigidBody(constructionInfo);

        // configure the rigid body
        rigidBody.setCollisionFlags(rigidBody.getCollisionFlags() | CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
//        rigidBody.setCollisionFlags(rigidBody.getCollisionFlags() | CollisionFlags.CF_KINEMATIC_OBJECT);

        // a mass of zero indicates that the body is manually re-oriented
        // so the body's activation state shouldn't be managed by bullet
        if (mass == 0) {
            rigidBody.setCollisionFlags(rigidBody.getCollisionFlags() | CollisionFlags.CF_KINEMATIC_OBJECT);
            rigidBody.setActivationState(Collision.DISABLE_DEACTIVATION);
            rigidBody.setContactCallbackFlag(PhysicsSystem.Flags.ground);
            rigidBody.setContactCallbackFilter(0);
        }
        else {
            var numPhysicsEntities = Game.instance.engine.getEntitiesFor(ComponentFamilies.physics).size();
            rigidBody.setUserValue(numPhysicsEntities);
            rigidBody.setContactCallbackFlag(PhysicsSystem.Flags.object);
            rigidBody.setContactCallbackFilter(PhysicsSystem.Flags.ground);
        }

//        var object = gameObjectBuilders.get(GameObject.Type.random()).build();
//        {
//            object.transform.setFromEulerAngles(angleX, angleY, angleZ);
//            object.transform.trn(posX, posY, posZ);
//            object.rigidBody.proceedToTransform(object.transform);
//            object.rigidBody.setUserValue(gameObjects.size);
//            object.rigidBody.setCollisionFlags(object.rigidBody.getCollisionFlags() | CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
//            object.rigidBody.setContactCallbackFlag(OBJECT_FLAG);
//            object.rigidBody.setContactCallbackFilter(SOFT_FLAG);
//        }
//        gameObjects.add(object);
//        dynamicsWorld.addRigidBody(object.rigidBody);

        this.outOfBounds = false;
    }

    public btCollisionShape shape() {
        return collisionShape;
    }

    @Override
    public void dispose() {
        motionState.dispose();
        constructionInfo.dispose();
        rigidBody.dispose();
    }

    @RequiredArgsConstructor
    private class MotionState extends btMotionState {
        final Matrix4 transform;
        final Vector3 translation = new Vector3();

        @Override
        public void getWorldTransform(Matrix4 worldTrans) {
            // called when Bullet needs to know the current transform of an object
            worldTrans.set(transform);
        }

        @Override
        public void setWorldTransform(Matrix4 worldTrans) {
            // called when Bullet has transformed an object
            transform.set(worldTrans);
            transform.getTranslation(translation);

            // handle objects that fall out of the world (y pos < threshold)
            if (translation.y < -10) {
                PhysicsComponent.this.outOfBounds = true;
            }
        }
    }

}
