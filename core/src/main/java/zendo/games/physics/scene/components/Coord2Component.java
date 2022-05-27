package zendo.games.physics.scene.components;

import com.badlogic.ashley.core.Component;

public record Coord2Component(int x, int y) implements Component {
    public boolean equals(int x, int y) {
        return (this.x == x) && (this.y == y);
    }
}
