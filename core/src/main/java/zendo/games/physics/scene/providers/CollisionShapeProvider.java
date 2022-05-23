package zendo.games.physics.scene.providers;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import lombok.RequiredArgsConstructor;

import static zendo.games.physics.scene.providers.CollisionShapeProvider.Type.*;

public class CollisionShapeProvider implements Provider<btCollisionShape> {

    public enum Type { rect, box, sphere, cone, capsule, cylinder }

    private final ObjectMap<Type, btCollisionShape> shapes = new ObjectMap<>();
    private final Array<btCollisionShape> customShapes = new Array<>();

    public CollisionShapeProvider() {
        shapes.put(rect,     new btBox2dShape(new Vector3(50f, 0f, 50f)));
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
    }

    @Override
    public btCollisionShape get(Object key) {
        if (key instanceof Type type) {
            return shapes.get(type);
        }
        throw new GdxRuntimeException("No collision shape for specified key: " + key);
    }

    public btCollisionShape create(Builder builder) {
        var shape = builder.build();
        customShapes.add(shape);
        return shape;
    }

    @RequiredArgsConstructor
    public static class Builder {
        private final Type type;

        private final Vector3 halfExtents = new Vector3();
        private float radius = 0.5f;
        private float height = 1f;

        Builder halfExtents(float x, float y, float z) {
            this.halfExtents.set(x, y, z);
            return this;
        }
        Builder radius(float radius) {
            this.radius = radius;
            return this;
        }
        Builder height(float height) {
            this.height = height;
            return this;
        }

        btCollisionShape build() {
            return switch (type) {
                case rect     -> new btBox2dShape(halfExtents);
                case box      -> new btBoxShape(halfExtents);
                case sphere   -> new btSphereShape(radius);
                case cone     -> new btConeShape(radius, height);
                case capsule  -> new btCapsuleShape(radius, height);
                case cylinder -> new btCylinderShape(halfExtents);
            };
        }
    }

}
