package zendo.games.physics.scene.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;

public class ModelInstanceComponent extends ModelInstance implements Component {

    public ModelInstanceComponent(Model model) {
        super(model);
    }

    public ModelInstanceComponent(Model model, String nodeId) {
        super(model, nodeId);
    }

}
