package zendo.games.physics;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
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
	ModelBatch debugModelBatch;
	SpriteBatch spriteBatch;
	ShapeRenderer shapeRenderer;

	ColorAttribute ambientLightAttrib;
	DirectionalLight directionalLight;

	btDispatcher dispatcher;
	btConstraintSolver constraintSolver;
	btCollisionConfiguration collisionConfig;
	btBroadphaseInterface broadphase;
	btDynamicsWorld dynamicsWorld;
	Contacts contactListener;
	DebugDrawer debugDrawer;

	Model scene;
	Model terrainModel;
	GameObject ground;
	GameObject terrain;
	ModelInstance coords;

	private final int heightValueRows = 20;
	private final int heightValueCols = 20;
	private final float[] heights = new float[heightValueRows * heightValueCols];
	private float[] vertices;

	final Array<GameObject> gameObjects = new Array<>();
	final Array<GameObject> toBeRemoved = new Array<>();
	final ArrayMap<GameObject.Type, GameObject.Builder> gameObjectBuilders = new ArrayMap<>();

	final float MAX_SPAWN_TIME = 0.1f;
//	final float MAX_SPAWN_TIME = 1f;
	float spawnTime = MAX_SPAWN_TIME;

	final float speed = 160f;
//	final float speed = 60f;
	float angle = 0f;

	BitmapFont font;

	// ------------------------------------------------------------------------
	// Data structures
	// ------------------------------------------------------------------------

	public static class Config {
		public static final int fov = 67;
		public static final int width = 1280;
		public static final int height = 720;
		public static boolean bulletDebugDraw = false;
		public static boolean wireframeDebugDraw = false;
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
						.color.set(1f, 1f, 1f, 0.9f);
			} else if (match1) {
				((ColorAttribute) object1.materials.first()
						.get(ColorAttribute.Diffuse))
						.color.set(1f, 1f, 1f, 0.9f);
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

		modelBatch = new ModelBatch();
		debugModelBatch = new ModelBatch(new DefaultShaderProvider() {
			@Override
			protected Shader createShader(Renderable renderable) {
				return new WireframeShader(renderable, config);
			}
		});
		spriteBatch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();

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
		contactListener = new Contacts();

		debugDrawer = new DebugDrawer();
		debugDrawer.setSpriteBatch(spriteBatch);
		debugDrawer.setShapeRenderer(shapeRenderer);
		debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_DrawWireframe);

		dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, constraintSolver, collisionConfig);
		dynamicsWorld.setGravity(new Vector3(0f, -9.8f, 0f));
		dynamicsWorld.setDebugDrawer(debugDrawer);

		font = new BitmapFont();

		createScene();
	}

	public void update() {
		var delta = Math.min(1f / 30f, Gdx.graphics.getDeltaTime());

		if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
			Gdx.app.exit();
		}
		if (Gdx.input.isKeyJustPressed(Keys.NUM_1)) {
			Config.wireframeDebugDraw = !Config.wireframeDebugDraw;
		}
		if (Gdx.input.isKeyJustPressed(Keys.NUM_2)) {
			Config.bulletDebugDraw = !Config.bulletDebugDraw;
		}

		spawnTime -= delta;
		if (spawnTime <= 0) {
			spawnTime = MAX_SPAWN_TIME;
			spawnObject();
		}

		// move the ground
		angle = (angle + delta * speed) % 360f;
		ground.transform.setTranslation(0, 2f + MathUtils.sinDeg(angle) * 2.5f, 0f);

		dynamicsWorld.stepSimulation(delta, 5, 1f / 60f);

		// remove dead objects
		removeDeadGameObjects();

		camController.update();
	}

	@Override
	public void render() {
		update();

		ScreenUtils.clear(Color.SKY, true);

		if (Config.wireframeDebugDraw) {
			debugModelBatch.begin(camera);
			debugModelBatch.render(coords, env);
			debugModelBatch.render(gameObjects, env);
			debugModelBatch.end();
		} else {
			modelBatch.begin(camera);
			modelBatch.render(coords, env);
			modelBatch.render(gameObjects, env);
			modelBatch.end();
		}

		if (Config.bulletDebugDraw) {
			debugDrawer.begin(camera);
			dynamicsWorld.debugDrawWorld();
			debugDrawer.end();
		}

		spriteBatch.begin();
		font.setColor(Color.BLACK);
		font.draw(spriteBatch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 10, 20);
		font.setColor(Color.WHITE);
		spriteBatch.end();
	}

	@Override
	public void dispose() {
		gameObjects.forEach(GameObject::dispose);
		gameObjectBuilders.values().forEach(GameObject.Builder::dispose);

		gameObjects.clear();
		gameObjectBuilders.clear();

		broadphase.dispose();
		dispatcher.dispose();
		collisionConfig.dispose();
		constraintSolver.dispose();
		debugDrawer.dispose();
		contactListener.dispose();

		font.dispose();
		scene.dispose();
		terrainModel.dispose();
		modelBatch.dispose();
		debugModelBatch.dispose();
		spriteBatch.dispose();
		shapeRenderer.dispose();

		// TODO - this causes a crash for some reason,
		//  not a huge deal since this should only
		//  happen when the application is closing
		//  but it'd be good to know why and fix it
//		dynamicsWorld.dispose();
	}

	// ------------------------------------------------------------------------
	// Implementation Methods
	// ------------------------------------------------------------------------

	private void createScene() {
		MeshPartBuilder meshPartBuilder;

		var builder = new ModelBuilder();
		var attribs = Usage.Position | Usage.Normal | Usage.ColorUnpacked;

		builder.begin();
		{
			float radius = heightValueRows / 2f;
			builder.node().id = TERRAIN.name();
			meshPartBuilder = builder.part(TERRAIN.name(), GL20.GL_TRIANGLES, attribs, new Material(ColorAttribute.createDiffuse(Color.FOREST)));
			PatchShapeBuilder.build(meshPartBuilder,
					-radius, 0f, -radius,
					-radius, 0f,  radius,
					 radius, 0f,  radius,
					 radius, 0f, -radius,
					0f, 1f, 0f,
					heightValueRows,
					heightValueCols
			);
		}
		terrainModel = builder.end();

		// additional initialization for terrain mesh vertices
		{
			var terrain = terrainModel.getNode(TERRAIN.name());
			var mesh = terrain.parts.first().meshPart.mesh;

			// populate height values
			for (int y = 0; y < heightValueRows; y++) {
				for (int x = 0; x < heightValueCols; x++) {
					int index = y * heightValueRows + x;
					heights[index] = MathUtils.random(0f, 1f);
				}
			}

			// populate an array for mesh vertices
			final int numComponents = mesh.getVertexSize() / Float.BYTES;
			final int numFloats = mesh.getNumVertices() * numComponents;
			vertices = new float[numFloats];
			mesh.getVertices(vertices);

			// update vertex values (just height for now)
			// NOTE - collision shapes expect their corresponding model to be centered around the origin
			//  so the terrain needs to be shifted down by half a unit since the heights are between (0,1)
			float yOffsetToOrigin = -0.5f;
			for (int y = 0; y < heightValueRows; y++) {
				for (int x = 0; x < heightValueCols; x++) {
					int index = x + y * heightValueCols;
//					vertices[numComponents * index + 0] = // pos-x
					vertices[numComponents * index + 1] = heights[index] + yOffsetToOrigin; // pos-y
//					vertices[numComponents * index + 2] = // pos-z
					vertices[numComponents * index + 3] = 0.2f + heights[index];         // color-r
					vertices[numComponents * index + 4] = 0.2f + heights[index] * 1.1f;  // color-g
					vertices[numComponents * index + 5] = 0.2f + heights[index];         // color-b
//					vertices[numComponents * index + 6] = // color-a
//					vertices[numComponents * index + 7] = // normal-x
//					vertices[numComponents * index + 8] = // normal-y
//					vertices[numComponents * index + 9] = // normal-z
				}
			}
			mesh.updateVertices(0, vertices);
		}

		// build scene model
		builder.begin();
		{
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

			builder.node().id = COORDS.name();
			float axisLength = 10f;
			float capLength = 0.1f;
			float stemThickness = 0.2f;
			int divisions = 6;
			var coordMaterial = new Material(ColorAttribute.createDiffuse(Color.WHITE), new BlendingAttribute(0.2f));
			meshPartBuilder = builder.part(COORDS.name(), GL20.GL_TRIANGLES, attribs, coordMaterial);
			meshPartBuilder.setColor(Color.RED);
			ArrowShapeBuilder.build(meshPartBuilder, 0, 0, 0, axisLength, 0, 0, capLength, stemThickness, divisions);
			meshPartBuilder.setColor(Color.GREEN);
			ArrowShapeBuilder.build(meshPartBuilder, 0, 0, 0, 0, axisLength, 0, capLength, stemThickness, divisions);
			meshPartBuilder.setColor(Color.BLUE);
			ArrowShapeBuilder.build(meshPartBuilder, 0, 0, 0, 0, 0, axisLength, capLength, stemThickness, divisions);
		}
		scene = builder.end();

		coords = new ModelInstance(scene, COORDS.name());

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

		var terrainNodeParts = terrainModel.getNode(TERRAIN.name()).parts;
		var terrainMeshParts = new Array<MeshPart>();
		for (var node : terrainNodeParts) {
			terrainMeshParts.add(node.meshPart);
		}
		var triangleMeshShape = new btBvhTriangleMeshShape(terrainMeshParts);
		gameObjectBuilders.put(TERRAIN,  new GameObject.Builder(0f, terrainModel, TERRAIN.name(), triangleMeshShape));

		createGameObjects();
	}

	private void createGameObjects() {
		ground = gameObjectBuilders.get(GROUND).build();
		{
			ground.rigidBody.setCollisionFlags(ground.rigidBody.getCollisionFlags()
					| btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT);
			ground.rigidBody.setContactCallbackFlag(GROUND_FLAG);
			ground.rigidBody.setContactCallbackFilter(0);
			// NOTE - since this is moved manually the rigid body's activation state shouldn't be managed by Bullet
			ground.rigidBody.setActivationState(Collision.DISABLE_DEACTIVATION);
		}
		gameObjects.add(ground);
		dynamicsWorld.addRigidBody(ground.rigidBody);

		terrain = gameObjectBuilders.get(TERRAIN).build();
		{
			terrain.rigidBody.setCollisionFlags(terrain.rigidBody.getCollisionFlags()
					| btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT);
			terrain.rigidBody.setContactCallbackFlag(GROUND_FLAG);
			terrain.rigidBody.setContactCallbackFilter(0);
			// NOTE - since this is moved manually the rigid body's activation state shouldn't be managed by Bullet
			terrain.rigidBody.setActivationState(Collision.DISABLE_DEACTIVATION);
			terrain.transform.rotate(0f, 1f, 0f, 180f);
		}
		gameObjects.add(terrain);
		dynamicsWorld.addRigidBody(terrain.rigidBody);

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

	private void removeDeadGameObjects() {
//		Gdx.app.log("Main", "game objects: " + gameObjects.size);
		toBeRemoved.clear();
		for (int i = 0; i < gameObjects.size; i++) {
			var object = gameObjects.get(i);
			if (!object.isAlive) {
				dynamicsWorld.removeRigidBody(object.rigidBody);
				object.dispose();
				toBeRemoved.add(object);
			}
		}
		gameObjects.removeAll(toBeRemoved, true);

		// NOTE - removing from gameObjects breaks userValues,
		//  which are indices to lookup GameObjects in ContactListener
		//  so the userValues need to be updated after removal
		for (int i = 0; i < gameObjects.size; i++) {
			var object = gameObjects.get(i);
			object.rigidBody.setUserValue(i);
		}
	}

}