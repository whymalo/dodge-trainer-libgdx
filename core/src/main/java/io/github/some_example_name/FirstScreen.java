package io.github.some_example_name;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class FirstScreen implements Screen {

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;

    // Позиция «игрока» (2D)
    private float playerX = 100f;
    private float playerY = 100f;

    private float speed = 200f; // пикселей в секунду

    public FirstScreen() {
        // Конструктор экрана
    }

    @Override
    public void show() {
        // Вызывается при переходе на этот экран
        batch = new SpriteBatch();

        // Создаём ортокамеру. Для 2D‐проекта обычно setToOrtho(false).
        camera = new OrthographicCamera();
        // Допустим, условная виртуальная «камера» 800×480 (размер на ваш вкус)
        viewport = new FitViewport(800, 480, camera);

        // Центрируем камеру на старте
        camera.position.set(playerX, playerY, 0);
        camera.update();
    }

    @Override
    public void render(float delta) {
        // 1. Обновляем логику (ввод с клавиатуры, движение и т. д.)
        handleInput(delta);

        // 2. Фокусируем камеру на игроке (если хотим «следование»)
        camera.position.set(playerX, playerY, 0);
        camera.update();

        // 3. Очищаем экран
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 4. Рисуем спрайты
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        // Здесь нарисуйте вашего персонажа (спрайт, текстура)
        // Пример: batch.draw(texture, playerX - 16, playerY - 16);
        batch.end();
    }

    private void handleInput(float delta) {
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            playerY += speed * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            playerY -= speed * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            playerX -= speed * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            playerX += speed * delta;
        }
    }

    @Override
    public void resize(int width, int height) {
        // При изменении размера окна / экрана
        viewport.update(width, height);
        camera.update();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {
        // Вызывается при переходе с этого скрина на другой
    }

    @Override
    public void dispose() {
        // Освобождение ресурсов
        batch.dispose();
    }
}
