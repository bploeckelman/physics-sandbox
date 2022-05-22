package zendo.games.physics.scene;

import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.ArrowShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import zendo.games.physics.Game;
import zendo.games.physics.scene.components.ModelInstanceComponent;
import zendo.games.physics.scene.components.NameComponent;

public class Scene implements Disposable {

    private final Engine engine;
    private final Environment environment;

    private final Model model;

    private enum Nodes { axes, floor, box }

    public Scene(Engine engine) {
        this.engine = engine;

        this.environment = new Environment();
        this.environment.set(ColorAttribute.createAmbientLight(0.3f, 0.3f, 0.3f, 1f));

        var light = new DirectionalLight();
        light.set(Color.WHITE, -1f, -0.8f, -0.2f);
        this.environment.add(light);

        this.model = buildSceneModel();

        // create test entities for each node
        for (var node : Nodes.values()) {
            var name = node.name();
            engine.addEntity(engine.createEntity()
                    .add(new NameComponent(name))
                    .add(new ModelInstanceComponent(model, name))
            );
        }

        for (int i = 1; i < 10; i++) {
            var id = Nodes.box.name();
            var name = id + " " + i;
            var entity = engine.createEntity()
                    .add(new NameComponent(name))
                    .add(new ModelInstanceComponent(model, id));
            engine.addEntity(entity);

            entity.getComponent(ModelInstanceComponent.class)
                    .transform.setTranslation(i, 0, i);
        }
    }

    public Environment env() {
        return environment;
    }

    public void update(float delta) {
        // ...
    }

    @Override
    public void dispose() {
        model.dispose();
    }

    private Model buildSceneModel() {
        var builder = new ModelBuilder();
        builder.begin();
        {
            String id;
            var attribs = VertexAttributes.Usage.Position
                    | VertexAttributes.Usage.Normal
                    | VertexAttributes.Usage.ColorUnpacked
                    | VertexAttributes.Usage.TextureCoordinates;

            // --------------------------------------------
            // floor
            // --------------------------------------------
            var floorSize = 100f;
            var texture = Game.instance.assets.prototypeGridOrange;

            id = Nodes.floor.name();
            builder.node().id = id;
            var partBuilder = builder.part(id, GL20.GL_TRIANGLES, attribs,
                    new Material(
                              ColorAttribute.createDiffuse(Color.WHITE)
                            , ColorAttribute.createSpecular(Color.WHITE)
                            , FloatAttribute.createShininess(16f)
                            , TextureAttribute.createDiffuse(texture)
                    )
            );
            partBuilder.rect(
                    new MeshPartBuilder.VertexInfo().setPos(-floorSize / 2, 0f, -floorSize / 2).setNor(Vector3.Y).setCol(Color.WHITE).setUV(0, 0),
                    new MeshPartBuilder.VertexInfo().setPos(-floorSize / 2, 0f,  floorSize / 2).setNor(Vector3.Y).setCol(Color.WHITE).setUV(0, 10),
                    new MeshPartBuilder.VertexInfo().setPos( floorSize / 2, 0f,  floorSize / 2).setNor(Vector3.Y).setCol(Color.WHITE).setUV( 10, 10),
                    new MeshPartBuilder.VertexInfo().setPos( floorSize / 2, 0f, -floorSize / 2).setNor(Vector3.Y).setCol(Color.WHITE).setUV( 10, 0)
            );

            // --------------------------------------------
            // axes
            // --------------------------------------------
            var axisLength = 10f;
            var capLength = 0.1f;
            var stemThickness = 0.2f;
            var divisions = 6;

            id = Nodes.axes.name();
            builder.node().id = id;
            partBuilder = builder.part(id, GL20.GL_TRIANGLES, attribs,
                    new Material(ColorAttribute.createDiffuse(Color.WHITE))
            );
            partBuilder.setColor(Color.WHITE); SphereShapeBuilder.build(partBuilder, 1f, 1f, 1f, 10, 10);
            partBuilder.setColor(Color.RED);   ArrowShapeBuilder.build(partBuilder, 0, 0, 0, axisLength, 0, 0, capLength, stemThickness, divisions);
            partBuilder.setColor(Color.GREEN); ArrowShapeBuilder.build(partBuilder, 0, 0, 0, 0, axisLength, 0, capLength, stemThickness, divisions);
            partBuilder.setColor(Color.BLUE);  ArrowShapeBuilder.build(partBuilder, 0, 0, 0, 0, 0, axisLength, capLength, stemThickness, divisions);

            // --------------------------------------------
            // box
            // --------------------------------------------
            texture = Game.instance.assets.libgdxTexture;

            id = Nodes.box.name();
            builder.node().id = id;
            partBuilder = builder.part(id, GL20.GL_TRIANGLES, attribs,
                    new Material(
                            ColorAttribute.createDiffuse(Color.WHITE)
                            , ColorAttribute.createSpecular(Color.WHITE)
                            , TextureAttribute.createDiffuse(texture)
                    )
            );
            BoxShapeBuilder.build(partBuilder, 0.5f, 0.5f, 0.5f, 1f, 1f, 1f);
        }
        return builder.end();
    }

}
