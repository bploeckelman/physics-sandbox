package zendo.games.physics.scene.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.physics.bullet.linearmath.btVector3;
import com.badlogic.gdx.physics.bullet.softbody.btSoftBodyRigidBodyCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.softbody.btSoftBodyWorldInfo;
import com.badlogic.gdx.physics.bullet.softbody.btSoftRigidDynamicsWorld;
import com.badlogic.gdx.utils.Disposable;
import zendo.games.physics.Game;
import zendo.games.physics.scene.components.PhysicsComponent;
import zendo.games.physics.scene.components.utils.ComponentFamilies;
import zendo.games.physics.scene.components.utils.ComponentMappers;

public class PhysicsSystem extends EntitySystem implements EntityListener, Disposable {

    private static final String TAG = PhysicsSystem.class.getSimpleName();

    public static class Flags {
        public static int ground = 1 << 9;
        public static int object = 1 << 8;
    }

    private final ComponentMapper<PhysicsComponent> mapper = ComponentMappers.physics;

    private final btDispatcher dispatcher;
    private final btConstraintSolver constraintSolver;
    private final btCollisionConfiguration collisionConfig;
    private final btSoftBodyWorldInfo softBodyWorldInfo;
    private final btBroadphaseInterface broadphase;
    // TODO - more general? btCollisionWorld?
    private final btSoftRigidDynamicsWorld dynamicsWorld;

    private final Contacts contactListener;
    private final DebugDrawer debugDrawer;

    public PhysicsSystem() {
        collisionConfig = new btSoftBodyRigidBodyCollisionConfiguration();
        dispatcher = new btCollisionDispatcher(collisionConfig);
        broadphase = new btDbvtBroadphase();
        constraintSolver = new btSequentialImpulseConstraintSolver();

        softBodyWorldInfo = new btSoftBodyWorldInfo();
        softBodyWorldInfo.setDispatcher(dispatcher);
        softBodyWorldInfo.setBroadphase(broadphase);
        softBodyWorldInfo.getSparsesdf().Initialize();
        softBodyWorldInfo.setGravity(new btVector3(0f, -9.8f, 0f));

        dynamicsWorld = new btSoftRigidDynamicsWorld(dispatcher, broadphase, constraintSolver, collisionConfig);
        // TODO - set gravity here and in softBodyWorldInfo, or just one place?
        dynamicsWorld.setGravity(new Vector3(0f, -9.8f, 0f));

        contactListener = new Contacts();
        debugDrawer = new DebugDrawer();
        debugDrawer.setSpriteBatch(Game.instance.assets.batch);
        debugDrawer.setShapeRenderer(Game.instance.assets.shapeRenderer);
        debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_DrawWireframe | btIDebugDraw.DebugDrawModes.DBG_DrawContactPoints);
        dynamicsWorld.setDebugDrawer(debugDrawer);
    }

    @Override
    public void dispose() {
        collisionConfig.dispose();
        dispatcher.dispose();
        broadphase.dispose();
        constraintSolver.dispose();
        softBodyWorldInfo.dispose();
        contactListener.dispose();
        debugDrawer.dispose();

        // TODO - crash on world dispose, not sure why yet
//        dynamicsWorld.dispose();
    }

    @Override
    public void entityAdded(Entity entity) {
        var component = mapper.get(entity);
        dynamicsWorld.addRigidBody(component.rigidBody);
    }

    @Override
    public void entityRemoved(Entity entity) {
        var component = mapper.get(entity);
        if (component.rigidBody.isInWorld()) {
            dynamicsWorld.removeRigidBody(component.rigidBody);
        }
        if (!component.rigidBody.isDisposed()) {
            component.dispose();
        }
    }

    @Override
    public void update(float delta) {
        var engine = getEngine();
        var entities = engine.getEntitiesFor(ComponentFamilies.physics);
        for (var entity : entities) {
            var physics = mapper.get(entity);
            if (physics.outOfBounds) {
                engine.removeEntity(entity);
            }
        }

        dynamicsWorld.stepSimulation(delta, 5, 1f / 60f);
    }
    
    public void removeFromWorld(PhysicsComponent physics) {
        if (!physics.rigidBody.isInWorld()) return;
        dynamicsWorld.removeRigidBody(physics.rigidBody);
    }
    
    public void addToWorld(PhysicsComponent physics) {
        if (physics.rigidBody.isInWorld()) {
            Gdx.app.log(TAG, "Tried to add physics component to world when it was already added!");
            return;
        }
        dynamicsWorld.addRigidBody(physics.rigidBody);
    }

    public void renderDebug(Camera camera) {
        debugDrawer.begin(camera);
        dynamicsWorld.debugDrawWorld();
        debugDrawer.end();
    }

    static class Contacts extends ContactListener {
        @Override
        public boolean onContactAdded(int userValue0, int partId0, int index0, boolean match0,
                                      int userValue1, int partId1, int index1, boolean match1) {
//            var object0 = gameObjects.get(userValue0);
//            var object1 = gameObjects.get(userValue1);
//
//            if (match0) {
//                ((ColorAttribute) object0.materials.first()
//                        .get(ColorAttribute.Diffuse))
//                        .color.set(1f, 1f, 1f, 0.9f);
//            } else if (match1) {
//                ((ColorAttribute) object1.materials.first()
//                        .get(ColorAttribute.Diffuse))
//                        .color.set(1f, 1f, 1f, 0.9f);
//            }
            return true;
        }
    }

}
