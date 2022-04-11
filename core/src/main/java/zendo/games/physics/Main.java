package zendo.games.physics;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.ScreenUtils;

import static com.badlogic.gdx.Input.Keys;
import static com.badlogic.gdx.graphics.VertexAttributes.Usage;
import static zendo.games.physics.GameObject.Type.*;

public class Main extends ApplicationAdapter {

	private static final short OBJECT_FLAG = 1 << 8;
	private static final short GROUND_FLAG = 1 << 9;

	PerspectiveCamera camera;
	CameraInputController camController;

	Environment env;
	ModelBatch modelBatch;
	SpriteBatch spriteBatch;

	ColorAttribute ambientLightAttrib;
	DirectionalLight directionalLight;

	Contacts contactListener;
	btDispatcher dispatcher;
	btConstraintSolver constraintSolver;
	btCollisionConfiguration collisionConfig;
	btBroadphaseInterface broadphase;
	btDynamicsWorld dynamicsWorld;

	Model scene;
	GameObject ground;
	final Array<GameObject> gameObjects = new Array<>();
	final ArrayMap<GameObject.Type, GameObject.Builder> gameObjectBuilders = new ArrayMap<>();

	final float MAX_SPAWN_TIME = 0.5f;
	float spawnTime = MAX_SPAWN_TIME;

	BitmapFont font;

	// ------------------------------------------------------------------------
	// Data structures
	// ------------------------------------------------------------------------

	public static class Config {
		public static final int fov = 67;
		public static final int width = 1280;
		public static final int height = 720;
	}

	public class Contacts extends ContactListener {
		@Override
		public boolean onContactAdded(int userValue0, int partId0, int index0, boolean match0,
                                      int userValue1, int partId1, int index1, boolean match1) {
			var object0 = gameObjects.get(userValue0);
			var object1 = gameObjects.get(userValue1);

			if (match0) {
				((ColorAttribute) object0.materials.first()
						.get(ColorAttribute.Diffuse))
						.color.set(1f, 1f, 1f, 0.4f);
			} else if (match1) {
				((ColorAttribute) object1.materials.first()
						.get(ColorAttribute.Diffuse))
						.color.set(1f, 1f, 1f, 0.4f);
			}

			return true;
		}
	}

	// ------------------------------------------------------------------------
	// Lifecycle Methods
	// ------------------------------------------------------------------------

	@Override
	public void create() {
		Bullet.init();
		contactListener = new Contacts();

		modelBatch = new ModelBatch();
		spriteBatch = new SpriteBatch();

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
		broadphase = new btDbvtBroadphase();
		constraintSolver = new btSequentialImpulseConstraintSolver();
		dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, constraintSolver, collisionConfig);
		dynamicsWorld.setGravity(new Vector3(0f, -9.8f, 0f));

		font = new BitmapFont();

		createScene();
	}

	public void update() {
		var delta = Math.min(1f / 30f, Gdx.graphics.getDeltaTime());

		if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
			Gdx.app.exit();
		}

		spawnTime -= delta;
		if (spawnTime <= 0) {
			spawnTime = MAX_SPAWN_TIME;
			spawnObject();
		}

		dynamicsWorld.stepSimulation(delta, 5, 1f / 60f);

		// remove dead objects
		// NOTE - removing from gameObjects breaks the userValues that are used as indices to lookup GameObjects in ContactListener
		//  so they can't be removed from the gameObjects array for now
//		for (int i = gameObjects.size - 1; i >= 0; i--) {
//			var object = gameObjects.get(i);
//			if (!object.isAlive) {
//				dynamicsWorld.removeRigidBody(object.rigidBody);
//				object.isRemoved = true;
//				object.dispose();
//				gameObjects.removeIndex(i);
//			}
//		}

		camController.update();
	}

	@Override
	public void render() {
		update();

		ScreenUtils.clear(Color.SKY, true);

		modelBatch.begin(camera);
		modelBatch.render(gameObjects, env);
		modelBatch.end();

		spriteBatch.begin();
		spriteBatch.setColor(Color.BLACK);
		font.draw(spriteBatch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 10, 20);
		spriteBatch.setColor(Color.WHITE);
		spriteBatch.end();
	}

	@Override
	public void dispose() {
		gameObjects.forEach(GameObject::dispose);
		gameObjectBuilders.values().forEach(GameObject.Builder::dispose);

		gameObjects.clear();
		gameObjectBuilders.clear();

		// NOTE - order of disposal matters for the Bullet types since they hold native pointers via JNI
		dynamicsWorld.dispose();
		broadphase.dispose();
		dispatcher.dispose();
		collisionConfig.dispose();
		constraintSolver.dispose();

		contactListener.dispose();

		scene.dispose();
		modelBatch.dispose();
		spriteBatch.dispose();
	}

	// ------------------------------------------------------------------------
	// Implementation Methods
	// ------------------------------------------------------------------------

	private void createScene() {
		var builder = new ModelBuilder();
		builder.begin();
		{
			MeshPartBuilder meshPartBuilder;

			var attribs = Usage.Position | Usage.ColorPacked | Usage.Normal;

			// TODO - replace ground node with terrain mesh
			builder.node().id = GROUND.name();
			meshPartBuilder = builder.part(GROUND.name(), GL20.GL_TRIANGLES, attribs, new Material(ColorAttribute.createDiffuse(Color.RED)));
			BoxShapeBuilder.build(meshPartBuilder, 5f, 1f, 5f);

			builder.node().id = SPHERE.name();
			meshPartBuilder = builder.part(SPHERE.name(), GL20.GL_TRIANGLES, attribs, new Material(ColorAttribute.createDiffuse(Color.GREEN)));
			SphereShapeBuilder.build(meshPartBuilder, 1f, 1f, 1f, 10, 10);

			builder.node().id = BOX.name();
			meshPartBuilder = builder.part(BOX.name(), GL20.GL_TRIANGLES, attribs, new Material(ColorAttribute.createDiffuse(Color.BLUE)));
			BoxShapeBuilder.build(meshPartBuilder, 1f, 1f, 1f);

			builder.node().id = CONE.name();
			meshPartBuilder = builder.part(CONE.name(), GL20.GL_TRIANGLES, attribs, new Material(ColorAttribute.createDiffuse(Color.YELLOW)));
			ConeShapeBuilder.build(meshPartBuilder, 1f, 2f, 1f, 10);

			builder.node().id = CAPSULE.name();
			meshPartBuilder = builder.part(CAPSULE.name(), GL20.GL_TRIANGLES, attribs, new Material(ColorAttribute.createDiffuse(Color.CYAN)));
			CapsuleShapeBuilder.build(meshPartBuilder, 0.5f, 2f, 10);

			builder.node().id = CYLINDER.name();
			meshPartBuilder = builder.part(CYLINDER.name(), GL20.GL_TRIANGLES, attribs, new Material(ColorAttribute.createDiffuse(Color.MAGENTA)));
			CylinderShapeBuilder.build(meshPartBuilder, 1f, 2f, 1f, 10);
		}
		scene = builder.end();

		createGameObjectBuilders();
	}

	private void createGameObjectBuilders() {
		// TODO - this duplicates size parameters from ModelBuilder setup in createScene(), easy to get wrong so centralize
		gameObjectBuilders.put(GROUND,   new GameObject.Builder(0f, scene, GROUND.name(),   new btBoxShape(new Vector3(2.5f, 0.5f, 2.5f))));
		gameObjectBuilders.put(SPHERE,   new GameObject.Builder(1f, scene, SPHERE.name(),   new btSphereShape(0.5f)));
		gameObjectBuilders.put(BOX,      new GameObject.Builder(1f, scene, BOX.name(),      new btBoxShape(new Vector3(0.5f, 0.5f, 0.5f))));
		gameObjectBuilders.put(CONE,     new GameObject.Builder(1f, scene, CONE.name(),     new btConeShape(0.5f, 2f)));
		gameObjectBuilders.put(CAPSULE,  new GameObject.Builder(1f, scene, CAPSULE.name(),  new btCapsuleShape(0.5f, 1f)));
		gameObjectBuilders.put(CYLINDER, new GameObject.Builder(1f, scene, CYLINDER.name(), new btCylinderShape(new Vector3(0.5f, 1f, 0.5f))));

		createGameObjects();
	}

	private void createGameObjects() {
		ground = gameObjectBuilders.get(GROUND).build();
		{
			ground.rigidBody.setContactCallbackFlag(GROUND_FLAG);
			ground.rigidBody.setContactCallbackFilter(0);
		}
		gameObjects.add(ground);
		dynamicsWorld.addRigidBody(ground.rigidBody);

		int numStartingObjects = 5;
		for (int i = 0; i < numStartingObjects; i++) {
			spawnObject();
		}
	}

	private void spawnObject() {
		var object = gameObjectBuilders.get(GameObject.Type.random()).build();
		{
			object.transform.setFromEulerAngles(MathUtils.random(360f), MathUtils.random(360f), MathUtils.random(360f));
			object.transform.trn(MathUtils.random(-2.5f, 2.5f), MathUtils.random(10, 14f), MathUtils.random(-2.5f, 2.5f));
			object.rigidBody.proceedToTransform(object.transform);
			object.rigidBody.setUserValue(gameObjects.size);
			object.rigidBody.setCollisionFlags(object.rigidBody.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
			object.rigidBody.setContactCallbackFlag(OBJECT_FLAG);
			object.rigidBody.setContactCallbackFilter(GROUND_FLAG);
		}
		gameObjects.add(object);
		dynamicsWorld.addRigidBody(object.rigidBody);
	}

}