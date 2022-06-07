package zendo.games.physics.scene.factories;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.math.Vector3;
import zendo.games.physics.Assets;
import zendo.games.physics.Game;
import zendo.games.physics.scene.components.Coord2Component;
import zendo.games.physics.scene.components.NameComponent;
import zendo.games.physics.scene.components.PhysicsComponent;
import zendo.games.physics.scene.components.TileComponent;
import zendo.games.physics.scene.packs.MinigolfModels;
import zendo.games.physics.scene.providers.ModelProvider;
import zendo.games.physics.scene.systems.ProviderSystem;
import zendo.games.physics.screens.BaseScreen;

import java.util.Objects;

import static zendo.games.physics.scene.providers.CollisionShapeProvider.Type;

public class EntityFactory {

    // ------------------------------------------------------------------------

    public static Entity createFloor(Engine engine) {
        return createFloor(engine, true);
    }

    public static Entity createFloor(Engine engine, boolean addToEngine) {
        var providers = engine.getSystem(ProviderSystem.class);

        var entity = engine.createEntity();
        {
            var name = new NameComponent("floor");

            var size = 80f;
            var node = ModelProvider.Node.patch;
            var modelInstance = providers.modelProvider.createModelInstanceComponent(node);

            // save the initial transform before scaling for the physics component
            var transform = modelInstance.transform.cpy();
            modelInstance.transform.setToScaling(size, 1f, size);

            var collisionShape = providers.collisionShapeProvider
                    .builder(Type.rect, name.name())
                    .halfExtents(size / 2, 0, size / 2)
                    .build();
            var physics = new PhysicsComponent(0f, transform, collisionShape);

            entity.add(name);
            entity.add(modelInstance);
            entity.add(physics);
        }

        if (addToEngine) {
            engine.addEntity(entity);
        }

        return entity;
    }

    // ------------------------------------------------------------------------

    public static Entity createOriginAxes(Engine engine) {
        return createOriginAxes(engine, true);
    }

    public static Entity createOriginAxes(Engine engine, boolean addToEngine) {
        var providers = engine.getSystem(ProviderSystem.class);

        var entity = engine.createEntity();
        {
            var name = new NameComponent("origin");

            var node = ModelProvider.Node.axes;
            var modelInstance = providers.modelProvider.createModelInstanceComponent(node);

            entity.add(name);
            entity.add(modelInstance);
        }

        if (addToEngine) {
            engine.addEntity(entity);
        }

        return entity;
    }

    // ------------------------------------------------------------------------

    private static int numCratesSpawned = 0;

    // TODO - set optional initial position

    public static Entity createCrate(Engine engine, Vector3 position) {
        return createCrate(engine, position, true);
    }

    public static Entity createCrate(Engine engine, Vector3 position, boolean addToEngine) {
        var providers = engine.getSystem(ProviderSystem.class);

        var entity = engine.createEntity();
        {
            var name = new NameComponent("crate " + numCratesSpawned++);

            var node = ModelProvider.Node.cube;
            var modelInstance = providers.modelProvider.createModelInstanceComponent(node);

            var transform = modelInstance.transform;
            transform.setTranslation(position);
//            transform.translate(0, 15, 0);

            var material = modelInstance.getMaterial(node.name());
            material.get(TextureAttribute.class, TextureAttribute.Diffuse)
                    .textureDescription.texture = Game.instance.assets.crateTexture;

            var collisionShape = providers.collisionShapeProvider.get(Type.box);
            var physics = new PhysicsComponent(transform, collisionShape);

            entity.add(name);
            entity.add(modelInstance);
            entity.add(physics);
        }

        if (addToEngine) {
            engine.addEntity(entity);
        }

        return entity;
    }

    // ------------------------------------------------------------------------

    private static int numShotsSpawned = 0;

    private static final Vector3 pickEndPoint = new Vector3();

    public static Entity createShot(Engine engine, Camera camera) {
        return createShot(engine, camera, camera.viewportWidth / 2f, camera.viewportHeight / 2f);
    }

    public static Entity createShot(Engine engine, Camera camera, float screenX, float screenY) {
        return createShot(engine, camera, screenX, screenY, true);
    }

    public static Entity createShot(Engine engine, Camera camera, float screenX, float screenY, boolean addToEngine) {
        var providers = engine.getSystem(ProviderSystem.class);

        var entity = engine.createEntity();
        {
            var name = new NameComponent("shot " + numShotsSpawned++);

            // create model instance
            var node = ModelProvider.Node.sphere;
            var modelInstance = providers.modelProvider.createModelInstanceComponent(node);

            // modify material texture
            var material = modelInstance.getMaterial(node.name());
            material.get(TextureAttribute.class, TextureAttribute.Diffuse)
                    .textureDescription.texture = Game.instance.assets.metalTexture;

            // set initial transform
            var scale = 2f;
            var impulse = 30f;
            var pickRay = camera.getPickRay(screenX, screenY);
            pickRay.getEndPoint(pickEndPoint, camera.position.y);

            var angles = BaseScreen.vec3Pool.obtain().setZero();
            var position = BaseScreen.vec3Pool.obtain().set(
                    pickRay.origin.x + pickRay.direction.x * scale,
                    pickRay.origin.y + pickRay.direction.y * scale,
                    pickRay.origin.z + pickRay.direction.z * scale
            );

            var transform = modelInstance.transform;
            transform.setFromEulerAngles(angles.x, angles.y, angles.z);
            transform.setTranslation(position);

            // setup physics
            var collisionShape = providers.collisionShapeProvider.get(Type.sphere);
            var physics = new PhysicsComponent(transform, collisionShape);
            physics.rigidBody.setUserValue(numShotsSpawned);
            physics.rigidBody.proceedToTransform(transform);
            physics.rigidBody.applyCentralImpulse(pickRay.direction.scl(impulse));

            entity.add(name);
            entity.add(modelInstance);
            entity.add(physics);

            BaseScreen.vec3Pool.free(angles);
            BaseScreen.vec3Pool.free(position);
        }

        if (addToEngine) {
            engine.addEntity(entity);
        }

        return entity;
    }

    // ------------------------------------------------------------------------

    private static int numTiles = 0;

    // TODO - find a better place for this
    public static final float TILE_SIZE = 10f;

    // TODO - make a helper that converts between screenX,Y and tileX,Y

    public static Entity createTile(MinigolfModels modelType, Engine engine, Assets assets, int tileX, int tileY) {
        return createTile(modelType, engine, assets, tileX, tileY, true);
    }

    public static Entity createTile(MinigolfModels modelType, Engine engine, Assets assets, int tileX, int tileY, boolean addToEngine) {
        var providers = engine.getSystem(ProviderSystem.class);
        var vec3Pool = BaseScreen.vec3Pool;

        var entity = engine.createEntity();
        {
            var name = new NameComponent("Held Tile");
            var coord = new Coord2Component(tileX, tileY);

            var offset = TILE_SIZE / 2f;
            var x = coord.x() * TILE_SIZE;
            var z = coord.y() * TILE_SIZE;
            var position = vec3Pool.obtain().set(offset + x, 0, offset + z);
            var scaling = vec3Pool.obtain().set(TILE_SIZE, TILE_SIZE, TILE_SIZE);

            // create the model instance
            var modelKey = modelType.key();
            var models = providers.modelProvider;
            var model = models.getOrCreate(modelKey, assets);
            Objects.requireNonNull(model, "Failed to get model file '" + modelKey + "' from asset manager");

            // set the initial position and orientation of the model instance
            var modelInstance = models.createModelInstanceComponent(modelKey);
            modelInstance.transform.setToTranslation(position);

            // NOTE - scaling needs to be applied to the collision shape as well as the model instance
            //   but scaling the model instance and then using that to build the collision shape
            //   breaks collisions (due to not explicitly calling btCollisionShape.setLocalScaling()?)
            //   so just use the position to create the collision shape then scale the model instance separately
            var transform = modelInstance.transform.cpy();
            modelInstance.transform.scale(scaling.x, scaling.y, scaling.z);

            // setup physics
            var key = modelKey.substring(1, modelKey.indexOf('.')) + (numTiles++);
            var collisionShape = providers.collisionShapeProvider
                    .builder(Type.custom, key).model(model).build();

            // make sure collision shape has correct scale
            collisionShape.setLocalScaling(scaling);

            var physics = new PhysicsComponent(0f, transform, collisionShape);

            // TODO - make things like this into construction parameters of the physics component
            // manage the rigidBody translation manually
            // instead of letting bullet do it with the motion state
            physics.rigidBody.setMotionState(null);

            // NOTE - exporting a model as y-up orients the model instance correctly,
            //  but the collision shape built from the model's triangles is still oriented as z-up
            //  so either the physics body needs to be re-oriented or both physics and model instance do
            //  easier to do the physics body separately in case we don't need a physics body
            //  for any particular model
            // set initial position and orientation of physics body
            transform = physics.rigidBody.getWorldTransform();
            transform.rotate(Vector3.X, -90f);
            physics.rigidBody.setWorldTransform(transform);

            var tile = new TileComponent(tileX, tileY, 0f, modelType);

            entity.add(name);
            entity.add(coord);
            entity.add(modelInstance);
            entity.add(physics);
            entity.add(tile);

            vec3Pool.free(position);
            vec3Pool.free(scaling);
        }

        if (addToEngine) {
            engine.addEntity(entity);
        }

        return entity;
    }

    // ------------------------------------------------------------------------

}
