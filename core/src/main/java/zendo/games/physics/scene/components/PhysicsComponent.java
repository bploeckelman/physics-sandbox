package zendo.games.physics.scene.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
import com.badlogic.gdx.utils.Disposable;
import lombok.RequiredArgsConstructor;
import zendo.games.physics.scene.systems.PhysicsSystem;

import static com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;

public class PhysicsComponent implements Component, Disposable {

    private static final Vector3 localInteria = new Vector3();

    private final MotionState motionState;
    private final btCollisionShape collisionShape; // TODO - make provider?
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

        localInteria.setZero();
        if (mass > 0) {
            collisionShape.calculateLocalInertia(mass, localInteria);
        }

        this.constructionInfo = new btRigidBodyConstructionInfo(mass, motionState, collisionShape, localInteria);
        this.rigidBody = new btRigidBody(constructionInfo);

        // NOTE - probably don't need this if it's passed in on construction
//        rigidBody.setMotionState(motionState);

        // TODO - a bunch of these settings should be construction parameters somehow
//        rigidBody.setCollisionFlags(rigidBody.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT);
        rigidBody.setCollisionFlags(rigidBody.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
        rigidBody.setContactCallbackFlag(PhysicsSystem.Flags.ground);
        rigidBody.setContactCallbackFilter(0);

        if (mass == 0) {
            // NOTE - since this body is moved manually the rigid body's activation state shouldn't be managed by Bullet
            rigidBody.setActivationState(Collision.DISABLE_DEACTIVATION);
        }

//        var object = gameObjectBuilders.get(GameObject.Type.random()).build();
//        {
//            object.transform.setFromEulerAngles(angleX, angleY, angleZ);
//            object.transform.trn(posX, posY, posZ);
//            object.rigidBody.proceedToTransform(object.transform);
//            object.rigidBody.setUserValue(gameObjects.size);
//            object.rigidBody.setCollisionFlags(object.rigidBody.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
//            object.rigidBody.setContactCallbackFlag(OBJECT_FLAG);
//            object.rigidBody.setContactCallbackFilter(SOFT_FLAG);
//        }
//        gameObjects.add(object);
//        dynamicsWorld.addRigidBody(object.rigidBody);

        this.outOfBounds = false;
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
