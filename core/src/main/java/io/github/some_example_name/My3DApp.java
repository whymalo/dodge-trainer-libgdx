package io.github.some_example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class My3DApp extends ApplicationAdapter {

    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Model playerModel;
    private Model obstacleModel;
    private ModelInstance playerInstance;
    private Array<ModelInstance> obstacles;
    private Environment environment;

    private final Vector3 playerPosition = new Vector3();
    private float obstacleSpawnTimer = 0;
    private boolean gameOver = false;

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
        environment.add(new DirectionalLight()
            .set(1f, 1f, 1f, -0.5f, -1f, 0.5f));

        // Модели
        ModelBuilder modelBuilder = new ModelBuilder();

        // Игрок (красная сфера)
        playerModel = modelBuilder.createSphere(
            2f, 2f, 2f,
            20, 20,
            new Material(ColorAttribute.createDiffuse(0.8f, 0.3f, 0.3f, 1f)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );

        // Препятствия (желтые сферы)
        obstacleModel = modelBuilder.createSphere(
            2f, 2f, 2f,
            20, 20,
            new Material(ColorAttribute.createDiffuse(0.8f, 0.8f, 0.2f, 1f)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );

        playerInstance = new ModelInstance(playerModel);
        playerInstance.transform.translate(0f, 5f, 0f);

        obstacles = new Array<>();
    }

    private void spawnObstacle() {
        ModelInstance obstacle = new ModelInstance(obstacleModel);

        // Стартовая позиция за пределами экрана
        float angle = (float) (Math.random() * 360);
        float distance = 50f;
        obstacle.transform.translate(
            (float) (Math.cos(angle) * distance),
            5f,
            (float) (Math.sin(angle) * distance)
        );

        obstacles.add(obstacle);
    }

    private void checkCollisions() {
        playerInstance.transform.getTranslation(playerPosition);

        for(ModelInstance obstacle : obstacles) {
            Vector3 obstaclePos = new Vector3();
            obstacle.transform.getTranslation(obstaclePos);

            if(playerPosition.dst(obstaclePos) < 2f) { // Радиусы обеих сфер по 2f
                gameOver = true;
                Gdx.app.log("GAME OVER", "Collision detected!");
            }
        }
    }

    @Override
    public void render() {
        if(gameOver) return;

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // Спавн препятствий
        obstacleSpawnTimer += Gdx.graphics.getDeltaTime();
        if(obstacleSpawnTimer > 2f) {
            spawnObstacle();
            obstacleSpawnTimer = 0;
        }

        // Движение игрока
        float deltaTime = Gdx.graphics.getDeltaTime();
        float speed = 15f * deltaTime;

        Vector3 forward = new Vector3(camera.direction);
        forward.y = 0f;
        forward.nor();

        Vector3 right = new Vector3(forward).crs(camera.up).nor();

        if (Gdx.input.isKeyPressed(Keys.W)) playerInstance.transform.translate(new Vector3(forward).scl(speed));
        if (Gdx.input.isKeyPressed(Keys.S)) playerInstance.transform.translate(new Vector3(forward).scl(-speed));
        if (Gdx.input.isKeyPressed(Keys.A)) playerInstance.transform.translate(new Vector3(right).scl(-speed));
        if (Gdx.input.isKeyPressed(Keys.D)) playerInstance.transform.translate(new Vector3(right).scl(speed));

        // Движение препятствий к игроку
        playerInstance.transform.getTranslation(playerPosition);
        for(ModelInstance obstacle : obstacles) {
            Vector3 obstaclePos = new Vector3();
            obstacle.transform.getTranslation(obstaclePos);

            Vector3 direction = new Vector3(playerPosition).sub(obstaclePos).nor();
            obstacle.transform.translate(direction.scl(10f * deltaTime));
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
