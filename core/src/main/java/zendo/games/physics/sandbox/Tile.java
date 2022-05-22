package zendo.games.physics.sandbox;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.Disposable;

public class Tile implements Disposable {

    public record Coord(int x, int z) {}

    static final float DEFAULT_MASS = 0f;
    static final float DEFAULT_SCALE = 10f;

    Model model;
    String nodeId;
    ModelInstance instance;

    btRigidBody body;
    btCollisionShape shape;
    btRigidBody.btRigidBodyConstructionInfo info;

    float mass;
    public Coord coord;

    public Tile(Model model, String nodeId, int x, int z) {
        this.model = model;
        this.nodeId = nodeId;
        this.mass = DEFAULT_MASS;
        this.coord = new Coord(x, z);

        var scale = new Vector3(DEFAULT_SCALE, DEFAULT_SCALE, DEFAULT_SCALE);

        // NOTE - this 'fixes' an NPE when rendering the tile instances in the shadow model batch
        //   it fails to get a diffuse attribute from these models and NPEs in DefaultShader:234
        //   adding one manually with a plain white pixel texture seems to resolve it
        for (var material : this.model.materials) {
            var diffuse = material.get(TextureAttribute.Diffuse);
            if (diffuse == null) {
                material.set(TextureAttribute.createDiffuse(Main.pixel));
            }
        }

        this.instance = new ModelInstance(model, nodeId);
        this.instance.transform.scale(scale.x, scale.y, scale.z);
        this.instance.transform.trn(coord.x * scale.x, 0f, coord.z * scale.z);

        // add a blending attribute so alpha in the diffuse attribute is respected
        if (this.instance.materials.isEmpty()) {
            this.instance.materials.add(new Material());
        }
        this.instance.materials.first().set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));

        this.shape = new btBvhTriangleMeshShape(model.meshParts);
        this.shape.setLocalScaling(scale);

        this.info = new btRigidBody.btRigidBodyConstructionInfo(mass, null, shape, Vector3.Zero.cpy());
        this.body = new btRigidBody(info);

        this.body.setCollisionFlags(body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT);
        this.body.setContactCallbackFlag(Main.GROUND_FLAG);
        this.body.setContactCallbackFilter(0);
        // NOTE - since this is moved manually the rigid body's activation state shouldn't be managed by Bullet
        this.body.setActivationState(Collision.DISABLE_DEACTIVATION);
        // NOTE - motion state changes the world transform, so if we want to manually adjust it to fit, then motion state has to be disabled (so it'll only work for fixed shapes)
        var transform = body.getWorldTransform();
        transform.rotate(Vector3.X, -90f);
        transform.trn(coord.x * scale.x, 0f, coord.z * scale.z);
        this.body.setWorldTransform(transform);
    }

    @Override
    public void dispose() {
        shape.dispose();
        info.dispose();
        body.dispose();
        // NOTE - model disposal is handled by asset manager
    }

}
