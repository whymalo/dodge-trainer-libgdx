package io.github.some_example_name;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;

public class Main extends Game {
    @Override
    public void create() {
        // Устанавливаем наш скрин
        setScreen(new FirstScreen());
    }

    @Override
    public void dispose() {
        super.dispose();
        // Освобождаем ресурсы, если нужно
    }
}
