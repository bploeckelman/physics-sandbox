package zendo.games.physics.scene;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
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

    public DirectionalShadowLight shadowLight;

    private enum Nodes { axes, floor, box, sphere }

    public Scene(Engine engine) {
        this.engine = engine;

        this.environment = new Environment();
        this.environment.set(ColorAttribute.createAmbientLight(0.3f, 0.3f, 0.3f, 1f));

        var sunlight = new Color(244f / 255f, 233f / 255f, 155f / 255f, 1f);
        var lightDir = new Vector3(-1f, -0.8f, -0.2f);
        this.shadowLight = new DirectionalShadowLight(
                4096, 4096,
                100f, 100f,
                0.1f, 1000f
        );
        shadowLight.set(sunlight, lightDir);
        environment.add(shadowLight);
        environment.shadowMap = shadowLight;

        this.model = buildSceneModel();

        buildTestEntities();
    }

    public Environment env() {
        return environment;
    }

    public Model model() {
        return model;
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

            id = Nodes.floor.name();
            builder.node().id = id;
            var partBuilder = builder.part(id, GL20.GL_TRIANGLES, attribs,
                    new Material(
                              ColorAttribute.createDiffuse(Color.WHITE)
                            , ColorAttribute.createSpecular(Color.WHITE)
                            , FloatAttribute.createShininess(16f)
                            , TextureAttribute.createDiffuse(Game.instance.assets.prototypeGridOrange)
                    )
            );
            partBuilder.rect(
                    new MeshPartBuilder.VertexInfo().setPos(-floorSize / 2, 0f, -floorSize / 2).setNor(Vector3.Y).setCol(Color.WHITE).setUV(0, 0),
                    new MeshPartBuilder.VertexInfo().setPos(-floorSize / 2, 0f,  floorSize / 2).setNor(Vector3.Y).setCol(Color.WHITE).setUV(0, 100),
                    new MeshPartBuilder.VertexInfo().setPos( floorSize / 2, 0f,  floorSize / 2).setNor(Vector3.Y).setCol(Color.WHITE).setUV( 100, 100),
                    new MeshPartBuilder.VertexInfo().setPos( floorSize / 2, 0f, -floorSize / 2).setNor(Vector3.Y).setCol(Color.WHITE).setUV( 100, 0)
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
            id = Nodes.box.name();
            builder.node().id = id;
            partBuilder = builder.part(id, GL20.GL_TRIANGLES, attribs,
                    new Material(
                              ColorAttribute.createDiffuse(Color.WHITE)
                            , TextureAttribute.createDiffuse(Game.instance.assets.crateTexture)
                    )
            );
            BoxShapeBuilder.build(partBuilder, 0.5f, 0.5f, 0.5f, 1f, 1f, 1f);

            // --------------------------------------------
            // sphere
            // --------------------------------------------
            id = Nodes.sphere.name();
            builder.node().id = id;
            partBuilder = builder.part(id, GL20.GL_TRIANGLES, attribs,
                    new Material(
                              ColorAttribute.createDiffuse(Color.WHITE)
                            , ColorAttribute.createSpecular(Color.WHITE)
                            , FloatAttribute.createShininess(32f)
                            , TextureAttribute.createDiffuse(Game.instance.assets.metalTexture)
                    )
            );
            SphereShapeBuilder.build(partBuilder, 1f, 1f, 1f, 16, 16);
        }
        return builder.end();
    }

    private void buildTestEntities() {
        Entity entity;

        entity = engine.createEntity()
                .add(new NameComponent("floor"))
                .add(new ModelInstanceComponent(model, "floor"));
        engine.addEntity(entity);

        entity = engine.createEntity()
                .add(new NameComponent("axes"))
                .add(new ModelInstanceComponent(model, "axes"));
        engine.addEntity(entity);

//        for (var node : Nodes.values()) {
//            var name = node.name();
//            var entity = engine.createEntity()
//                    .add(new NameComponent(name))
//                    .add(new ModelInstanceComponent(model, name));
//            engine.addEntity(entity);
//
//            if (node == Nodes.sphere) {
//                entity.getComponent(ModelInstanceComponent.class)
//                        .transform.setTranslation(0.5f, 1.5f, 0.5f);
//            }
//        }
//
//        for (int i = 1; i < 10; i++) {
//            var id = Nodes.box.name();
//            var name = id + " " + i;
//            var entity = engine.createEntity()
//                    .add(new NameComponent(name))
//                    .add(new ModelInstanceComponent(model, id));
//            engine.addEntity(entity);
//
//            entity.getComponent(ModelInstanceComponent.class)
//                    .transform.setTranslation(i, 0, i);
//        }
//
//        {
//            var name = "model";
//            var objectModel = Game.instance.assets.mgr.get("start.g3db", Model.class);
//            var entity = engine.createEntity()
//                    .add(new NameComponent(name))
//                    .add(new ModelInstanceComponent(objectModel));
//            engine.addEntity(entity);
//
//            var tileSize = 10f;
//            entity.getComponent(ModelInstanceComponent.class)
//                    .transform.setTranslation(
//                            tileSize / 2f,
//                            0,
//                            tileSize / 2f + tileSize
//                    );
//        }
//
//        {
//            var name = "unit box";
//            var entity = engine.createEntity()
//                    .add(new NameComponent(name))
//                    .add(new ModelInstanceComponent(model, Nodes.box.name()));
//            engine.addEntity(entity);
//
//            var tileSize = 10f;
//            entity.getComponent(ModelInstanceComponent.class)
//                    .transform.setToTranslationAndScaling(
//                            -tileSize, 0, 0,
//                            tileSize, tileSize, tileSize
//                    );
//        }
//
//        {
//            var name = "unit sphere";
//            var entity = engine.createEntity()
//                    .add(new NameComponent(name))
//                    .add(new ModelInstanceComponent(model, Nodes.sphere.name()));
//            engine.addEntity(entity);
//
//            var tileSize = 10f;
//            entity.getComponent(ModelInstanceComponent.class)
//                    .transform.setToTranslationAndScaling(
//                            tileSize / 2f + tileSize,
//                            tileSize / 2f,
//                            tileSize / 2f + tileSize,
//                            tileSize, tileSize, tileSize
//                    );
//        }
    }

}
