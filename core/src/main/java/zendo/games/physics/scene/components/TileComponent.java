package zendo.games.physics.scene.components;

import com.badlogic.ashley.core.Component;
import lombok.AllArgsConstructor;
import zendo.games.physics.scene.packs.MinigolfModels;

@AllArgsConstructor
public class TileComponent implements Component {

    public int xCoord;
    public int zCoord;
    public float yRotation;
    public MinigolfModels modelType;

}
