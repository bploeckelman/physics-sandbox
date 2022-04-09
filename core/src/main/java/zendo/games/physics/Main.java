package zendo.games.physics;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.BulletBase;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

import static com.badlogic.gdx.Input.*;
import static com.badlogic.gdx.graphics.VertexAttributes.*;

public class Main extends ApplicationAdapter {

	PerspectiveCamera camera;
	CameraInputController camController;

	Environment env;
	ModelBatch batch;

	ColorAttribute ambientLightAttrib;
	DirectionalLight directionalLight;

	btDispatcher dispatcher;
	btCollisionConfiguration collisionConfig;

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

	public static class Instances {
		public static final Array<ModelInstance> all = new Array<>();
		public static ModelInstance ball;
		public static ModelInstance ground;
	}

	public static class CollisionShapes {
		public static final Array<btCollisionShape> all = new Array<>();
		public static btCollisionShape ball;
		public static btCollisionShape ground;
	}

	public static class CollisionObjects {
		public static final Array<btCollisionObject> all = new Array<>();
		public static btCollisionObject ball;
		public static btCollisionObject ground;
	}

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

		// model instances
		Instances.ball = new ModelInstance(Models.ball);
		Instances.ground = new ModelInstance(Models.ground);
		Instances.all.addAll(Instances.ball, Instances.ground);

		Instances.ball.transform.setToTranslation(0f, 9f, 0f);

		// collision shapes
		CollisionShapes.ball = new btSphereShape(0.5f);
		CollisionShapes.ground = new btBoxShape(new Vector3(2.5f, 0.5f, 2.5f));

		CollisionShapes.all.addAll(CollisionShapes.ball, CollisionShapes.ground);

		// collision objects
		CollisionObjects.ball = new btCollisionObject();
		CollisionObjects.ground = new btCollisionObject();

		CollisionObjects.all.addAll(CollisionObjects.ball, CollisionObjects.ground);

		CollisionObjects.ball.setCollisionShape(CollisionShapes.ball);
		CollisionObjects.ball.setWorldTransform(Instances.ball.transform);

		CollisionObjects.ground.setCollisionShape(CollisionShapes.ground);
		CollisionObjects.ground.setWorldTransform(Instances.ground.transform);
	}

	boolean collision = false;
	public void update() {
		var delta = Math.min(1f / 30f, Gdx.graphics.getDeltaTime());

		if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
			Gdx.app.exit();
		}

		if (!collision) {
			Instances.ball.transform.translate(0f, -delta, 0f);
			CollisionObjects.ball.setWorldTransform(Instances.ball.transform);
			collision = checkCollision();
		}

		camController.update();
	}

	private boolean checkCollision() {
		var co0 = new CollisionObjectWrapper(CollisionObjects.ball);
		var co1 = new CollisionObjectWrapper(CollisionObjects.ground);

		var ci = new btCollisionAlgorithmConstructionInfo();
		ci.setDispatcher1(dispatcher);

		var algo = new btSphereBoxCollisionAlgorithm(null, ci, co0.wrapper, co1.wrapper, false);

		var info = new btDispatcherInfo();
		var result = new btManifoldResult(co0.wrapper, co1.wrapper);

		algo.processCollision(co0.wrapper, co1.wrapper, info, result);

		var r = result.getPersistentManifold().getNumContacts() > 0;

		result.dispose();
		info.dispose();
		algo.dispose();
		ci.dispose();
		co0.dispose();
		co1.dispose();

		return r;
	}

	@Override
	public void render() {
		update();

		ScreenUtils.clear(Color.SKY, true);

		batch.begin(camera);
		batch.render(Instances.all, env);
		batch.end();
	}

	@Override
	public void dispose() {
		Models.all.forEach(Model::dispose);
		CollisionShapes.all.forEach(BulletBase::dispose);
		CollisionObjects.all.forEach(BulletBase::dispose);
		collisionConfig.dispose();
		dispatcher.dispose();
		batch.dispose();
	}

}