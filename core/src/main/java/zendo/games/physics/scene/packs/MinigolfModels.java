package zendo.games.physics.scene.packs;

public enum MinigolfModels implements ModelPack {
      block
    , bump_up
    , bump_up_walls
    , bump_down
    , bump_down_walls
    , castle
    , corner
    , corner_inner
    , corner_square_a
    , crest
    , end
    , gap
    , hill_corner
    , hill_round
    , hill_square
    , hole_open
    , hole_round
    , hole_square
    , narrow_block
    , narrow_round
    , narrow_square
    , obstacle_block
    , obstacle_diamond
    , obstacle_triangle
    , open
    , ramp_a
    , ramp_b
    , ramp_c
    , ramp_d
    , ramp_sharp
    , ramp_square
    , round_corner_a
    , round_corner_b
    , round_corner_c
    , side
    , split
    , split_t
    , split_walls_to_open
    , start
    , straight
    , tunnel_double
    , tunnel_narrow
    , tunnel_wide
    , wall_left
    , wall_right
    , walls_to_open
//    , windmill // TODO - the model doesn't import quite correctly, probably because it has multiple parts (blades vs house)
    ;

      public MinigolfModels next() {
          var index = ordinal();
          return values()[(index + 1) % values().length];
      }

      public MinigolfModels prev() {
          var index = ordinal();
          var prevIndex = index - 1;
          if (prevIndex < 0) {
              prevIndex = values().length + prevIndex;
          }
          return values()[prevIndex % values().length];
      }

      @Override
      public String prefix() {
          return "minigolf/";
      }

      @Override
      public String suffix() {
          return ".g3dj";
      }

      @Override
      public String modelName() {
          return name().replaceAll("_", "-");
      }

}
