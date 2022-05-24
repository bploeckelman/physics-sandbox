package zendo.games.physics.scene.providers;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;

import java.util.Objects;

import static zendo.games.physics.scene.providers.CollisionShapeProvider.Type.*;

public class CollisionShapeProvider implements Provider<btCollisionShape> {

    private static final String TAG = CollisionShapeProvider.class.getSimpleName();

    public enum Type { custom, rect, box, sphere, cone, capsule, cylinder }

    private final ObjectMap<Type, btCollisionShape> shapes = new ObjectMap<>();
    private final ObjectMap<String, btCollisionShape> customShapes = new ObjectMap<>();

    public CollisionShapeProvider() {
        shapes.put(rect,     new btBox2dShape(new Vector3(10f, 0f, 10f)));
        shapes.put(box,      new btBoxShape(new Vector3(0.5f, 0.5f, 0.5f)));
        shapes.put(sphere,   new btSphereShape(0.5f));
        shapes.put(cone,     new btConeShape(0.5f, 1f));
        shapes.put(capsule,  new btCapsuleShape(0.5f, 1f));
        shapes.put(cylinder, new btCylinderShape(new Vector3(0.5f, 1f, 0.5f)));
    }

    @Override
    public void dispose() {
        for (var shape : shapes.values()) {
            if (!shape.isDisposed()) {
                shape.dispose();
            }
        }
        shapes.clear();

        for (var shape : customShapes.values()) {
            if (!shape.isDisposed()) {
                shape.dispose();
            }
        }
        customShapes.clear();
    }

    /**
     * Retrieve the CollisionShape associated with the supplied key
     * The key can be either:
     * - CollisionShapeProvider.Type, which identifies an existing primitive shape
     * - String, which may identify a custom shape
     * @param key the identifier for the desired shape
     * @return the specified shape
     */
    @Override
    public btCollisionShape get(Object key) {
        btCollisionShape result = null;

        // NOTE - java 17 (preview) and higher supports switch over type
        if (key instanceof Type type) {
            result = shapes.get(type);
        } else if (key instanceof String keyStr && customShapes.containsKey(keyStr)) {
            result = customShapes.get(keyStr);
        }

        return Objects.requireNonNull(result, "No collision shape found for specified key: '" + key + "'");
    }

    // ------------------------------------------------------------------------

    public CollisionShapeBuilder builder(Type type, String key) {
        return new CollisionShapeBuilder(type, key);
    }

    public class CollisionShapeBuilder {

        private static final String TAG = CollisionShapeBuilder.class.getSimpleName();

        private final Type type;
        private final String key;
        private final Vector3 halfExtents = new Vector3();

        private Model model = null;
        private String nodeId = null;
        private float radius = 0.5f;
        private float height = 1f;

        private CollisionShapeBuilder(Type type, String key) {
            throwIfKeyInUse(key);
            this.type = type;
            this.key = key;
        }

        public CollisionShapeBuilder model(Model model) {
            this.model = model;
            return this;
        }

        public CollisionShapeBuilder node(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public CollisionShapeBuilder halfExtents(float x, float y, float z) {
            this.halfExtents.set(x, y, z);
            return this;
        }
        public CollisionShapeBuilder radius(float radius) {
            this.radius = radius;
            return this;
        }
        public CollisionShapeBuilder height(float height) {
            this.height = height;
            return this;
        }

        public btCollisionShape build() {
            return switch (type) {
                case rect     -> new btBox2dShape(halfExtents);
                case box      -> new btBoxShape(halfExtents);
                case sphere   -> new btSphereShape(radius);
                case cone     -> new btConeShape(radius, height);
                case capsule  -> new btCapsuleShape(radius, height);
                case cylinder -> new btCylinderShape(halfExtents);
                case custom   -> {
                    Objects.requireNonNull(model, "Unable to build collision shape, missing required value 'model'");

                    // collect mesh parts from the specified node if there is one
                    var meshParts = (nodeId == null) ? model.meshParts : new Array<MeshPart>();
                    if (nodeId != null) {
                        for (var nodePart : model.getNode(nodeId).parts) {
                            meshParts.add(nodePart.meshPart);
                        }
                    }

                    var shape = new btBvhTriangleMeshShape(meshParts);
                    customShapes.put(key, shape);
                    yield shape;
                }
            };
        }
    }

    // ------------------------------------------------------------------------

    private void throwIfKeyInUse(String key) {
        if (customShapes.containsKey(key)) {
            throw new GdxRuntimeException("Failed to create collision shape, key '" + key + "' is already in use");
        }
    }

}
