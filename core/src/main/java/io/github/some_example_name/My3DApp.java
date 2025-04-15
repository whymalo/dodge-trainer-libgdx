package io.github.some_example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.UBJsonReader;

public class My3DApp extends ApplicationAdapter {

    private PerspectiveCamera camera;
    private ModelBatch modelBatch;

    // Для игрока
    private Model playerModel;  // Ваша модель в формате g3db
    private ModelInstance playerInstance;

    // Для препятствий
    private Model obstacleModel;
    private Array<ModelInstance> obstacles;

    private Environment environment;
    private final Vector3 playerPosition = new Vector3();
    private float obstacleSpawnTimer = 0;
    private boolean gameOver = false;

    // Настройки игры
    private static final float PLAYER_SPEED = 400f;       // Нормальная скорость игрока
    private static final float OBSTACLE_SPEED = 370f;     // Скорость препятствий
    private static final float COLLISION_DISTANCE = 2.5f;// Дистанция для коллизии
    private static final float PLAYER_SCALE = 0.035f;
    private static final float OBSTACLE_SCALE = 0.02f;
    private static final float OBSTACLE_SPAWN_INTERVAL = 3f; // Интервал спавна

    @Override
    public void create() {
        modelBatch = new ModelBatch();

        // Настройка камеры
        camera = new PerspectiveCamera(
            70,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight()
        );
        camera.position.set(0f, 25f, 10f);
        camera.lookAt(0f, 0f, 0f);
        camera.near = 1f;
        camera.far = 300f;
        camera.update();

        // Освещение
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight()
            .set(0.8f, 0.8f, 0.8f, -0.5f, -1f, 0.5f));

        // 1. ЗАГРУЗКА МОДЕЛИ ИГРОКА --------------------------
        ModelLoader loader = new G3dModelLoader(new UBJsonReader());
        playerModel = loader.loadModel(Gdx.files.internal("models/Pensive_Glance_0414202339_texture.g3db"));

        // Настройка игрока
        playerInstance = new ModelInstance(playerModel);
        playerInstance.transform
            .setToTranslation(0f, 5f, 0f)
            .scale(PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);

        // 2. ПРЕПЯТСТВИЯ (оставляем сферы для примера) --------
        ModelLoader obstacleLoader = new G3dModelLoader(new UBJsonReader());
        obstacleModel = obstacleLoader.loadModel(Gdx.files.internal("models/Stickman_Yandex_0414211935_texture.g3db"));

        obstacles = new Array<>();
    }

    private void spawnObstacle() {
        ModelInstance obstacle = new ModelInstance(obstacleModel);

        float angle = (float) (Math.random() * 360);
        float distance = 20f; // Уменьшил дистанцию спавна
        obstacle.transform.translate(
            (float) (Math.cos(angle) * distance),
            5f,
            (float) (Math.sin(angle) * distance)
        ).scale(OBSTACLE_SCALE, OBSTACLE_SCALE, OBSTACLE_SCALE);

        obstacles.add(obstacle);
    }

    private void checkCollisions() {
        playerInstance.transform.getTranslation(playerPosition);

        for(ModelInstance obstacle : obstacles) {
            Vector3 obstaclePos = new Vector3();
            obstacle.transform.getTranslation(obstaclePos);

            if(playerPosition.dst(obstaclePos) < COLLISION_DISTANCE) {
                gameOver = true;
                Gdx.app.log("GAME OVER", "Collision detected!");
            }
        }
    }

    @Override
    public void render() {
        if(gameOver) return;

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.05f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // Спавн препятствий
        obstacleSpawnTimer += Gdx.graphics.getDeltaTime();
        if(obstacleSpawnTimer > OBSTACLE_SPAWN_INTERVAL) {
            spawnObstacle();
            obstacleSpawnTimer = 0;
        }

        // Движение игрока
        float deltaTime = Gdx.graphics.getDeltaTime();
        float currentSpeed = PLAYER_SPEED * deltaTime;

        Vector3 forward = new Vector3(camera.direction);
        forward.y = 0f;
        forward.nor();

        Vector3 right = new Vector3(forward).crs(camera.up).nor();

        if (Gdx.input.isKeyPressed(Keys.W)) playerInstance.transform.translate(new Vector3(forward).scl(currentSpeed));
        if (Gdx.input.isKeyPressed(Keys.S)) playerInstance.transform.translate(new Vector3(forward).scl(-currentSpeed));
        if (Gdx.input.isKeyPressed(Keys.A)) playerInstance.transform.translate(new Vector3(right).scl(-currentSpeed));
        if (Gdx.input.isKeyPressed(Keys.D)) playerInstance.transform.translate(new Vector3(right).scl(currentSpeed));

        // Движение препятствий к игроку
        playerInstance.transform.getTranslation(playerPosition);
        for(ModelInstance obstacle : obstacles) {
            Vector3 obstaclePos = new Vector3();
            obstacle.transform.getTranslation(obstaclePos);

            Vector3 direction = new Vector3(playerPosition).sub(obstaclePos).nor();
            obstacle.transform.translate(direction.scl(OBSTACLE_SPEED * deltaTime));
        }

        checkCollisions();

        // Отрисовка
        camera.update();
        modelBatch.begin(camera);
        modelBatch.render(playerInstance, environment);
        for(ModelInstance obstacle : obstacles) {
            modelBatch.render(obstacle, environment);
        }
        modelBatch.end();
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        playerModel.dispose();
        obstacleModel.dispose();
    }
}
