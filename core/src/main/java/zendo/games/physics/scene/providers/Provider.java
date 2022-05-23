package zendo.games.physics.scene.providers;

import com.badlogic.gdx.utils.Disposable;

public interface Provider<T> extends Disposable {
    T get(Object key);
}
