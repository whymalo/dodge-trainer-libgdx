package io.github.some_example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.UBJsonReader;

public class My3DApp extends ApplicationAdapter {

    private Model floorModel;
    private ModelInstance floorInstance;

    private PerspectiveCamera camera;
    private ModelBatch modelBatch;

    private Model playerModel;
    private ModelInstance playerInstance;

    private Model obstacleModel;
    private Array<ObstacleInstance> obstacles;

    private Model laserModel;
    private Array<LaserInstance> lasers;

    private Environment environment;
    private final Vector3 playerPosition = new Vector3();
    private float obstacleSpawnTimer = 0;
    private boolean gameOver = false;

    private static final float PLAYER_SPEED = 30f;
    private static final float COLLISION_DISTANCE = 2.5f;
    private static final float PLAYER_SCALE = 0.035f;
    private static final float OBSTACLE_SCALE = 0.02f;
    private static final float OBSTACLE_SPAWN_INTERVAL = 1.3f;
    private static final float BOUNDS_X = 70f;
    private static final float BOUNDS_Z = 70f;

    private BitmapFont font;
    private SpriteBatch spriteBatch;
    private float survivalTime = 0;

    private Array<ModelInstance> wallInstances;

    @Override
    public void create() {
        modelBatch = new ModelBatch();

        camera = new PerspectiveCamera(70, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, 60f, 25f);
        camera.lookAt(0f, 10f, 0f);
        camera.near = 1f;
        camera.far = 300f;
        camera.update();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f));
        environment.add(new DirectionalLight().set(0.4f, 0.4f, 0.4f, -0.5f, -1f, 0.5f));

        ModelBuilder modelBuilder = new ModelBuilder();
        Texture floorTexture = new Texture("textures/vurnari_screen.png");
        floorModel = modelBuilder.createRect(
            -70f, 0, 30f,
            70f, 0, 30f,
            85f, 0, -70f,
            -85f, 0, -70f,
            0, 1, 0,
            new Material(TextureAttribute.createDiffuse(floorTexture)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates
        );
        floorInstance = new ModelInstance(floorModel);

        wallInstances = new Array<>();
        Texture wallTexture = new Texture("textures/vurnari_screen.png");
        Material wallMat = new Material(TextureAttribute.createDiffuse(wallTexture));

        Model wallModel = modelBuilder.createBox(140f, 10f, 1f, wallMat, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        wallInstances.add(new ModelInstance(wallModel, 0, 5f, -55f)); // back
        wallInstances.add(new ModelInstance(wallModel, 0, 5f, 85f)); // front

        wallModel = modelBuilder.createBox(1f, 10f, 140f, wallMat, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        wallInstances.add(new ModelInstance(wallModel, -70f, 5f, 15f)); // left
        wallInstances.add(new ModelInstance(wallModel, 70f, 5f, 15f));  // right

        ModelLoader loader = new G3dModelLoader(new UBJsonReader());
        playerModel = loader.loadModel(Gdx.files.internal("models/Pensive_Glance_0414202339_texture.g3db"));
        obstacleModel = loader.loadModel(Gdx.files.internal("models/Stickman_Yandex_0414211935_texture.g3db"));
        laserModel = loader.loadModel(Gdx.files.internal("models/low_poly_lime_0415161736_texture.g3db"));

        Texture playerTexture = new Texture(Gdx.files.internal("textures/Image_0.jpg"));
        Texture obstacleTexture = new Texture(Gdx.files.internal("textures/texture_0.png"));
        Texture laserTexture = new Texture(Gdx.files.internal("textures/low_poly_lime_0415161736_texture.png"));

        for (Material mat : playerModel.materials) mat.set(TextureAttribute.createDiffuse(playerTexture));
        for (Material mat : obstacleModel.materials) mat.set(TextureAttribute.createDiffuse(obstacleTexture));
        for (Material mat : laserModel.materials) mat.set(TextureAttribute.createDiffuse(laserTexture));

        playerInstance = new ModelInstance(playerModel);
        playerInstance.transform.set(new Matrix4().setToTranslation(0f, 5f, 0f).scale(PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE));

        obstacles = new Array<>();
        lasers = new Array<>();

        font = new BitmapFont();
        spriteBatch = new SpriteBatch();
    }

    private static class ObstacleInstance {
        ModelInstance instance;
        float speed;
        float lifetime = 15f;

        ObstacleInstance(ModelInstance instance, float speed) {
            this.instance = instance;
            this.speed = speed;
        }
    }

    private static class LaserInstance {
        ModelInstance instance;
        Vector3 direction;
        float speed;
        float lifetime = 10f;

        LaserInstance(ModelInstance instance, Vector3 direction, float speed) {
            this.instance = instance;
            this.direction = direction;
            this.speed = speed;
        }
    }

    private void spawnObstacle() {
        if (obstacles.size >= 3) return;
        ModelInstance obstacle = new ModelInstance(obstacleModel);
        float angle = (float) (Math.random() * 360);
        float distance = 50f;
        float x = (float) Math.cos(angle) * distance;
        float z = (float) Math.sin(angle) * distance;

        obstacle.transform.set(new Matrix4().setToTranslation(x, 5f, z).scale(OBSTACLE_SCALE, OBSTACLE_SCALE, OBSTACLE_SCALE));
        float speed = 10f + (float) Math.random() * 20f;
        obstacles.add(new ObstacleInstance(obstacle, speed));
    }

    private void spawnLaser() {
        ModelInstance laser = new ModelInstance(laserModel);

        float angle = (float) (Math.random() * 360);
        float distance = 60f;
        float x = (float) Math.cos(angle) * distance;
        float z = (float) Math.sin(angle) * distance;
        Vector3 startPos = new Vector3(x, 5f, z);

        Vector3 dir = new Vector3(playerPosition).sub(startPos).nor();

        laser.transform.set(new Matrix4().setToLookAt(dir, new Vector3(0, 1, 0)).inv()
            .setTranslation(startPos).scale(OBSTACLE_SCALE, OBSTACLE_SCALE, OBSTACLE_SCALE));

        float speed = 30f + (float) Math.random() * 40f;
        lasers.add(new LaserInstance(laser, dir, speed));
    }

    private void checkCollisions() {
        playerInstance.transform.getTranslation(playerPosition);
        for (ObstacleInstance obstacle : obstacles) {
            Vector3 pos = new Vector3();
            obstacle.instance.transform.getTranslation(pos);
            if (playerPosition.dst(pos) < COLLISION_DISTANCE) {
                gameOver = true;
                Gdx.app.log("GAME OVER", "Collision with obstacle!");
            }
        }
    }

    private void restartGame() {
        survivalTime = 0;
        gameOver = false;
        obstacles.clear();
        lasers.clear();

        playerInstance.transform.set(new Matrix4().setToTranslation(0f, 5f, 0f).scale(PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE));
    }

    @Override
    public void render() {
        if (!gameOver) survivalTime += Gdx.graphics.getDeltaTime();
        else if (Gdx.input.isKeyJustPressed(Keys.R)) restartGame();

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.05f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        float deltaTime = Gdx.graphics.getDeltaTime();
        obstacleSpawnTimer += deltaTime;

        if (!gameOver && obstacleSpawnTimer > OBSTACLE_SPAWN_INTERVAL) {
            if (Math.random() < 0.8f) spawnLaser();
            else if (obstacles.size < 3) spawnObstacle();
            obstacleSpawnTimer = 0;
        }

        Vector3 forward = new Vector3(camera.direction);
        forward.y = 0;
        forward.nor();
        Vector3 right = new Vector3(forward).crs(camera.up).nor();
        Vector3 moveVector = new Vector3();

        if (Gdx.input.isKeyPressed(Keys.W)) moveVector.add(forward.scl(PLAYER_SPEED * deltaTime));
        if (Gdx.input.isKeyPressed(Keys.S)) moveVector.add(forward.scl(-PLAYER_SPEED * deltaTime));
        if (Gdx.input.isKeyPressed(Keys.A)) moveVector.add(right.scl(-PLAYER_SPEED * deltaTime));
        if (Gdx.input.isKeyPressed(Keys.D)) moveVector.add(right.scl(PLAYER_SPEED * deltaTime));

        if (!moveVector.isZero() && !gameOver) {
            Vector3 moveDir = new Vector3(moveVector).nor();
            Vector3 pos = new Vector3();
            playerInstance.transform.getTranslation(pos);
            pos.add(moveVector);

            pos.x = Math.max(-BOUNDS_X + 2f, Math.min(BOUNDS_X - 2f, pos.x));
            pos.z = Math.max(-BOUNDS_Z + 15f + 2f, Math.min(BOUNDS_Z - 2f, pos.z));

            Matrix4 newTransform = new Matrix4().setToLookAt(moveDir, new Vector3(0, 1, 0)).inv()
                .setTranslation(pos).scale(PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);

            playerInstance.transform.set(newTransform);
        }

        playerInstance.transform.getTranslation(playerPosition);
        if (!gameOver) {
            Array<ObstacleInstance> obstaclesToRemove = new Array<>();
            for (ObstacleInstance obstacle : obstacles) {
                Vector3 pos = new Vector3();
                obstacle.instance.transform.getTranslation(pos);
                Vector3 dir = new Vector3(playerPosition).sub(pos).nor();
                Vector3 movement = new Vector3(dir).scl(obstacle.speed * deltaTime);
                Vector3 newPos = pos.add(movement);
                obstacle.lifetime -= deltaTime;
                if (obstacle.lifetime <= 0) {
                    obstaclesToRemove.add(obstacle);
                    continue;
                }
                obstacle.instance.transform.set(new Matrix4().setToLookAt(dir, new Vector3(0, 1, 0)).inv()
                    .setTranslation(newPos).scale(OBSTACLE_SCALE, OBSTACLE_SCALE, OBSTACLE_SCALE));
            }
            obstacles.removeAll(obstaclesToRemove, true);

            Array<LaserInstance> lasersToRemove = new Array<>();
            for (LaserInstance laser : lasers) {
                Vector3 currentPos = new Vector3();
                laser.instance.transform.getTranslation(currentPos);
                Vector3 newPos = currentPos.add(new Vector3(laser.direction).scl(laser.speed * deltaTime));
                laser.lifetime -= deltaTime;
                if (laser.lifetime <= 0 || newPos.len() > 200f) {
                    lasersToRemove.add(laser);
                    continue;
                }
                if (newPos.dst(playerPosition) < COLLISION_DISTANCE) {
                    gameOver = true;
                    Gdx.app.log("GAME OVER", "Hit by laser!");
                }
                laser.instance.transform.set(new Matrix4().setToLookAt(laser.direction, new Vector3(0, 1, 0)).inv()
                    .setTranslation(newPos).scale(OBSTACLE_SCALE, OBSTACLE_SCALE, OBSTACLE_SCALE));
            }
            lasers.removeAll(lasersToRemove, true);
        }

        checkCollisions();
        camera.update();

        modelBatch.begin(camera);
        modelBatch.render(floorInstance, environment);
        for (ModelInstance wall : wallInstances) modelBatch.render(wall, environment);
        modelBatch.render(playerInstance, environment);
        for (ObstacleInstance obstacle : obstacles) modelBatch.render(obstacle.instance, environment);
        for (LaserInstance laser : lasers) modelBatch.render(laser.instance, environment);
        modelBatch.end();

        spriteBatch.begin();
        font.draw(spriteBatch, "Score: " + (int) survivalTime, 20, Gdx.graphics.getHeight() - 20);
        if (gameOver) {
            font.draw(spriteBatch, "GAME OVER", Gdx.graphics.getWidth() / 2f - 50, Gdx.graphics.getHeight() / 2f + 20);
            font.draw(spriteBatch, "Press R to Restart", Gdx.graphics.getWidth() / 2f - 70, Gdx.graphics.getHeight() / 2f - 10);
        }
        spriteBatch.end();
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        playerModel.dispose();
        obstacleModel.dispose();
        laserModel.dispose();
        font.dispose();
        spriteBatch.dispose();
        floorModel.dispose();
    }
}
