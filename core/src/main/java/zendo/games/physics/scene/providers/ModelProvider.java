package zendo.games.physics.scene.providers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import zendo.games.physics.Game;

public class ModelProvider implements Provider<Model> {

    public enum Node { floor }

    // TODO - create a 'ModelPack' that includes a bunch of loaded models that are accessible through some interface
    //   ie. minigolf tile pack

    private final Model model;

    public ModelProvider() {
        // create a scene model with a bunch of standard primitive nodes
        var modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        {
            var attribs = VertexAttributes.Usage.Position
                    | VertexAttributes.Usage.Normal
                    | VertexAttributes.Usage.ColorUnpacked
                    | VertexAttributes.Usage.TextureCoordinates;
            var material = new Material(
                      ColorAttribute.createDiffuse(Color.WHITE)
                    , ColorAttribute.createSpecular(Color.WHITE)
                    , FloatAttribute.createShininess(16f)
                    , TextureAttribute.createDiffuse(Game.instance.assets.prototypeGridOrange)
            );

            modelBuilder.node().id = Node.floor.name();
            var builder = modelBuilder.part(Node.floor.name(), GL20.GL_TRIANGLES, attribs, material);
            BoxShapeBuilder.build(builder, 0f, 0f, 0f, 1f, 1f, 1f);
        }
        this.model = modelBuilder.end();
    }

    @Override
    public Model get(Object key) {
        return null;
    }

    @Override
    public void dispose() {
        model.dispose();
    }

}
