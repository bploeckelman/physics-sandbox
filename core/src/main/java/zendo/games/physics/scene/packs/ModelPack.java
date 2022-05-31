package zendo.games.physics.scene.packs;

public interface ModelPack {

    String prefix();
    String suffix();
    String modelName();
    default String key() {
        return prefix() + modelName() + suffix();
    }

}
