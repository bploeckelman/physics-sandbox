package zendo.games.physics.scene.providers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.*;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import zendo.games.physics.Assets;
import zendo.games.physics.Config;
import zendo.games.physics.scene.components.ModelInstanceComponent;

import java.util.Objects;

import static zendo.games.physics.scene.providers.ModelProvider.Node.*;

public class ModelProvider implements Provider<Model> {

    public enum Node { axes, floor, patch, cube, sphere, capsule, cone, cylinder, frustum }

    private final Model model;
    private final ObjectMap<String, Model> customModels = new ObjectMap<>();

    // TODO - create 'ModelPack' structure that includes externally created models
    //   ie. minigolf tile pack

    public ModelProvider(Assets assets) {
        this.model = buildSceneModel(assets);
    }

    @Override
    public void dispose() {
        model.dispose();

        for (var model : customModels.values()) {
            model.dispose();
        }
        customModels.clear();
    }

    /**
     * Retrieve the Model associated with the supplied key
     * The key can be either:
     * - ModelProvider.Node, which identifies a primitive Node in the scene model
     * - String, which may identify a custom Model
     * @param key the identifier for the desired Model
     * @return the specified Model
     */
    @Override
    public Model get(Object key) {
        Model result = null;

        // NOTE - java 17 (preview) and higher supports switch over type
        if (key instanceof Node) {
            result = model;
        } else if (key instanceof String customKey) {
            result = customModels.get(customKey);
        }

        return Objects.requireNonNull(result, "No model found for specified key: '" + key + "'");
    }

    public ModelInstanceComponent createModelInstanceComponent(Object key) {
        if (key instanceof Node node) {
            return new ModelInstanceComponent(model, node.name());
        } else if (key instanceof String customKey) {
            // TODO - how to support accessing named nodes in custom models?
            var model = customModels.get(customKey);
            Objects.requireNonNull(model, "No model found for specified key: '" + key + "'");
            return new ModelInstanceComponent(model);
        } else {
            throw new GdxRuntimeException("Failed to create model instance: invalid key '" + key + "'");
        }
    }

    public Model create(String key, Model model) {
        if (customModels.containsKey(key)) {
            throw new GdxRuntimeException("Failed to create Model, key '" + key + "' is already in use");
        }

        customModels.put(key, model);
        return model;
    }

    private Model buildSceneModel(Assets assets) {
        // create a scene model with a bunch of standard primitive nodes
        var modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        {
            var attribs = VertexAttributes.Usage.Position
                    | VertexAttributes.Usage.Normal
                    | VertexAttributes.Usage.ColorUnpacked
                    | VertexAttributes.Usage.TextureCoordinates;
            var baseMaterial = new Material(
                      ColorAttribute.createDiffuse(Color.WHITE)
                    , ColorAttribute.createSpecular(Color.WHITE)
                    , FloatAttribute.createShininess(16f)
                    , TextureAttribute.createDiffuse(assets.prototypeGridOrange)
            );
            var unit = 1f;

            String name;
            MeshPartBuilder builder;

            name = axes.name();
            {
                var scale = 10f;
                var axesLength = unit * scale;
                var axesCapLength = 0.2f;
                var axesDivisions = 6;
                var axesStemThickness = 0.2f;
                var material = new Material(ColorAttribute.createDiffuse(Color.WHITE));
                material.id = name;

                modelBuilder.node().id = name;
                builder = modelBuilder.part(name, GL20.GL_TRIANGLES, attribs, material);
                builder.setColor(Color.WHITE); SphereShapeBuilder.build(builder, unit, unit, unit, 10, 10);
                builder.setColor(Color.RED);   ArrowShapeBuilder.build(builder, 0, 0, 0, axesLength, 0, 0, axesCapLength, axesStemThickness, axesDivisions);
                builder.setColor(Color.GREEN); ArrowShapeBuilder.build(builder, 0, 0, 0, 0, axesLength, 0, axesCapLength, axesStemThickness, axesDivisions);
                builder.setColor(Color.BLUE);  ArrowShapeBuilder.build(builder, 0, 0, 0, 0, 0, axesLength, axesCapLength, axesStemThickness, axesDivisions);
            }

            // TODO - probably remove, the only thing this gets that 'patch' doesn't is repeated textures
            name = floor.name();
            {
                var size = 100f;
                var material = baseMaterial.copy();
                material.id = name;
                material.get(TextureAttribute.class, TextureAttribute.Diffuse)
                        .textureDescription.texture = assets.prototypeGridOrange;

                modelBuilder.node().id = name;
                builder = modelBuilder.part(name, GL20.GL_TRIANGLES, attribs, material);
                builder.rect(
                        new MeshPartBuilder.VertexInfo().setPos(-size / 2, 0f, -size / 2).setNor(Vector3.Y).setCol(Color.WHITE).setUV(0, 0),
                        new MeshPartBuilder.VertexInfo().setPos(-size / 2, 0f, +size / 2).setNor(Vector3.Y).setCol(Color.WHITE).setUV(0, size),
                        new MeshPartBuilder.VertexInfo().setPos(+size / 2, 0f, +size / 2).setNor(Vector3.Y).setCol(Color.WHITE).setUV(size, size),
                        new MeshPartBuilder.VertexInfo().setPos(+size / 2, 0f, -size / 2).setNor(Vector3.Y).setCol(Color.WHITE).setUV(size, 0)
                );
            }

            // NOTE - this could be quite small depending on 'unit', likely need to scale it for use
            // TODO - extend PatchShapeBuilder to allow for different UV scales than 0..1 (for repeat/clamp)
            name = patch.name();
            {
                var patchDivisions = 10;
                var material = baseMaterial.copy();
                material.id = name;
                // customize material if desired...

                modelBuilder.node().id = name;
                builder = modelBuilder.part(name, GL20.GL_TRIANGLES, attribs, material);
                PatchShapeBuilder.build(builder,
                        -unit / 2f, 0f, +unit / 2f,
                        +unit / 2f, 0f, +unit / 2f,
                        +unit / 2f, 0f, -unit / 2f,
                        -unit / 2f, 0f, -unit / 2f,
                        0, 1, 0,
                        patchDivisions, patchDivisions
                );
            }

            name = cube.name();
            {
                var material = baseMaterial.copy();
                material.id = name;
                // customize material if desired...

                modelBuilder.node().id = name;
                builder = modelBuilder.part(name, GL20.GL_TRIANGLES, attribs, material);
                BoxShapeBuilder.build(builder, 0, 0, 0, unit, unit, unit);
            }

            name = sphere.name();
            {
                var divisions = 12;
                var material = baseMaterial.copy();
                material.id = name;
                // customize material if desired...

                modelBuilder.node().id = name;
                builder = modelBuilder.part(name, GL20.GL_TRIANGLES, attribs, material);
                SphereShapeBuilder.build(builder,
                        unit, unit, unit,
                        divisions, divisions
                );
            }

            name = capsule.name();
            {
                var radius = unit / 2f;
                var height = unit;
                var divisions = 12;
                var material = baseMaterial.copy();
                material.id = name;
                // customize material if desired...

                modelBuilder.node().id = name;
                builder = modelBuilder.part(name, GL20.GL_TRIANGLES, attribs, material);
                CapsuleShapeBuilder.build(builder, radius, height, divisions);
            }

            modelBuilder.node().id = cone.name();
            {
                var divisions = 12;
                var material = baseMaterial.copy();
                material.id = name;
                // customize material if desired...

                builder = modelBuilder.part(cone.name(), GL20.GL_TRIANGLES, attribs, material);
                ConeShapeBuilder.build(builder, unit, unit, unit, divisions);
            }

            name = cylinder.name();
            {
                var divisions = 12;
                var material = baseMaterial.copy();
                material.id = name;
                // customize material if desired...

                modelBuilder.node().id = name;
                builder = modelBuilder.part(name, GL20.GL_TRIANGLES, attribs, material);
                CylinderShapeBuilder.build(builder, unit, unit, unit, divisions);
            }

            name = frustum.name();
            {
                var material = baseMaterial.copy();
                material.id = name;
                // customize material if desired...

                modelBuilder.node().id = name;
                builder = modelBuilder.part(name, GL20.GL_LINES, attribs, material);
                FrustumShapeBuilder.build(builder, new PerspectiveCamera(67f, Config.width, Config.height));
            }
        }
        return modelBuilder.end();
    }

}
