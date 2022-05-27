package zendo.games.physics.utils;

import com.badlogic.gdx.Gdx;
import com.strongjoshua.console.CommandExecutor;
import com.strongjoshua.console.annotation.ConsoleDoc;
import zendo.games.physics.Game;
import zendo.games.physics.scene.components.utils.ComponentFamilies;
import zendo.games.physics.scene.components.utils.ComponentMappers;

public class ConsoleCommandExecutor extends CommandExecutor {

    public boolean isObjectSpawningEnabled = false;

    @ConsoleDoc(description = "Toggles whether to repeatedly spawn objects or not.")
    public final void spawn() {
        isObjectSpawningEnabled = !isObjectSpawningEnabled;
    }

    @ConsoleDoc(description = "Lists all named entities currently in the scene.")
    public final void entities() {
        var engine = Game.instance.engine;

        var str = new StringBuilder();
        var namedEntities = engine.getEntitiesFor(ComponentFamilies.names);
        var first = true;
        for (var entity : namedEntities) {
            var name = ComponentMappers.name.get(entity);
            if (!first) {
                str.append(", ");
            }
            str.append(name.name());
            first = false;

            var coord = ComponentMappers.coord2.get(entity);
            if (coord != null) {
                str.append(": ").append(coord);
            }
        }

        console.log(str.toString());
    }

    @ConsoleDoc(description = "Quits the application")
    public final void quit() {
        Gdx.app.exit();
    }

}
