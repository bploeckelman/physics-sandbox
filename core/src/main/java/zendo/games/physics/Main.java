package zendo.games.physics;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.BulletBase;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;
import lombok.RequiredArgsConstructor;

import static com.badlogic.gdx.Input.Keys;
import static com.badlogic.gdx.graphics.VertexAttributes.Usage;

public class Main extends ApplicationAdapter {

	PerspectiveCamera camera;
	CameraInputController camController;

	Environment env;
	ModelBatch batch;

	ColorAttribute ambientLightAttrib;
	DirectionalLight directionalLight;

	btDispatcher dispatcher;
	btCollisionConfiguration collisionConfig;

	Array<GameObject> gameObjects = new Array<>();

	// ------------------------------------------------------------------------
	// Data structures
	// ------------------------------------------------------------------------

	public static class Config {
		public static final int fov = 67;
		public static final int width = 1280;
		public static final int height = 720;
	}

	public static class Models {
		public static final Array<Model> all = new Array<>();
		public static Model ball;
		public static Model ground;
	}

	public static class CollisionShapes {
		public static final Array<btCollisionShape> all = new Array<>();
		public static btCollisionShape ball;
		public static btCollisionShape ground;
	}

	// TODO - should the entire scene be a single model and the various components just be nodes in that model?

	public static class GameObject extends ModelInstance implements Disposable {

		public final btCollisionObject body;

		public boolean moving;

		public GameObject(Model model, btCollisionShape shape) {
			super(model);
			body = new btCollisionObject();
			body.setCollisionShape(shape);
		}

		@Override
		public void dispose() {
			body.dispose();
		}

		@RequiredArgsConstructor
		public static class Builder implements Disposable {

			public final Model model;
			public final btCollisionShape shape;

			public GameObject build() {
				return new GameObject(model, shape);
			}

			@Override
			public void dispose() {
				shape.dispose();
			}

		}

	}

	final ArrayMap<String, GameObject.Builder> builders = new ArrayMap<>();


	// ------------------------------------------------------------------------
	// Methods
	// ------------------------------------------------------------------------

	@Override
	public void create() {
		Bullet.init();

		batch = new ModelBatch();

		ambientLightAttrib = ColorAttribute.createAmbientLight(0.3f, 0.3f, 0.3f, 1f);
		directionalLight = new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f);

		env = new Environment();
		env.set(ambientLightAttrib);
		env.add(directionalLight);

		camera = new PerspectiveCamera(Config.fov, Config.width, Config.height);
		camera.position.set(3f, 7f, 10f);
		camera.lookAt(0f, 4f, 0f);
		camera.update();

		camController = new CameraInputController(camera);
		Gdx.input.setInputProcessor(camController);

		collisionConfig = new btDefaultCollisionConfiguration();
		dispatcher = new btCollisionDispatcher(collisionConfig);

		createGameObjects();
	}

	private void createGameObjects() {
		// models
		var builder = new ModelBuilder();
		var attribs = Usage.Position | Usage.ColorPacked | Usage.Normal;
		var ballMaterial = new Material(ColorAttribute.createDiffuse(Color.RED));
		var groundMaterial = new Material(ColorAttribute.createDiffuse(Color.GREEN));

		Models.ball = builder.createSphere(1f, 1f, 1f, 10, 10, ballMaterial, attribs);
		Models.ground = builder.createBox(5f, 1f, 5f, groundMaterial, attribs);
		Models.all.addAll(Models.ball, Models.ground);

		// collision shapes
		CollisionShapes.ball = new btSphereShape(0.5f);
		CollisionShapes.ground = new btBoxShape(new Vector3(2.5f, 0.5f, 2.5f));

		CollisionShapes.all.addAll(CollisionShapes.ball, CollisionShapes.ground);

		// populate game object builders
		builders.put("ball", new GameObject.Builder(Models.ball, CollisionShapes.ball));
		builders.put("ground", new GameObject.Builder(Models.ground, CollisionShapes.ground));

		// create game objects
		// NOTE: important to add the ground first, as we'll be referencing it by gameObjects.first() later
		var ground = builders.get("ground").build();
		gameObjects.add(ground);

		int numBalls = 5;
		for (int i = 0; i < numBalls; i++) {
			var ball = builders.get("ball").build();
			ball.moving = true;
			ball.transform.setToTranslation(MathUtils.random(-2.5f, 2.5f), MathUtils.random(6f, 10f), MathUtils.random(-2.5f, 2.5f));
			ball.body.setWorldTransform(ball.transform);
			gameObjects.add(ball);
		}
	}

	public void update() {
		var delta = Math.min(1f / 30f, Gdx.graphics.getDeltaTime());

		if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
			Gdx.app.exit();
		}

		final float speed = 9.8f;
		for (var obj : gameObjects) {
			if (obj.moving) {
				obj.transform.trn(0f, -speed * delta, 0f);
				obj.body.setWorldTransform(obj.transform);
				var ground = gameObjects.first();
				if (checkCollision(obj.body, ground.body)) {
					obj.moving = false;
				}
			}
		}

		camController.update();
	}

	// TODO - several allocations per update, pool some of these types?
	private boolean checkCollision(btCollisionObject object1, btCollisionObject object2) {
		var co0 = new CollisionObjectWrapper(object1);
		var co1 = new CollisionObjectWrapper(object2);

		var ci = new btCollisionAlgorithmConstructionInfo();
		ci.setDispatcher1(dispatcher);

		var algo = dispatcher.findAlgorithm(co0.wrapper, co1.wrapper, ci.getManifold(), ebtDispatcherQueryType.BT_CONTACT_POINT_ALGORITHMS);

		var info = new btDispatcherInfo();
		var result = new btManifoldResult(co0.wrapper, co1.wrapper);

		algo.processCollision(co0.wrapper, co1.wrapper, info, result);

		var r = result.getPersistentManifold().getNumContacts() > 0;

		result.dispose();
		info.dispose();
		ci.dispose();
		co0.dispose();
		co1.dispose();
		dispatcher.freeCollisionAlgorithm(algo.getCPointer());

		return r;
	}

	@Override
	public void render() {
		update();

		ScreenUtils.clear(Color.SKY, true);

		batch.begin(camera);
		batch.render(gameObjects, env);
		batch.end();
	}

	@Override
	public void dispose() {
		Models.all.forEach(Model::dispose);
		CollisionShapes.all.forEach(BulletBase::dispose);
		collisionConfig.dispose();
		dispatcher.dispose();
		batch.dispose();

		gameObjects.forEach(GameObject::dispose);
		builders.values().forEach(GameObject.Builder::dispose);
	}

}