package zendo.games.physics.utils;

import com.badlogic.gdx.Gdx;
import com.strongjoshua.console.CommandExecutor;
import com.strongjoshua.console.annotation.ConsoleDoc;

public class ConsoleCommandExecutor extends CommandExecutor {

    @ConsoleDoc(description = "Quits the application")
    public final void quit() {
        Gdx.app.exit();
    }

}
