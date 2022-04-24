package zendo.games.physics;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.utils.*;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.linearmath.LinearMath;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.physics.bullet.linearmath.btVector3;
import com.badlogic.gdx.physics.bullet.softbody.btSoftBody;
import com.badlogic.gdx.physics.bullet.softbody.btSoftBodyRigidBodyCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.softbody.btSoftBodyWorldInfo;
import com.badlogic.gdx.physics.bullet.softbody.btSoftRigidDynamicsWorld;
import com.badlogic.gdx.utils.*;

import static com.badlogic.gdx.Input.Keys;
import static com.badlogic.gdx.graphics.VertexAttributes.Usage;
import static zendo.games.physics.GameObject.Type.*;

public class Main extends ApplicationAdapter {

	public static final short OBJECT_FLAG = 1 << 7;
	public static final short SOFT_FLAG   = 1 << 8;
	public static final short GROUND_FLAG = 1 << 9;

	PerspectiveCamera camera;
	CameraInputController camController;

	Environment env;
	ModelBatch modelBatch;
	ModelBatch shadowBatch;
	ModelBatch debugModelBatch;
	SpriteBatch spriteBatch;
	ShapeRenderer shapeRenderer;

	ColorAttribute ambientLightAttrib;
	DirectionalShadowLight directionalShadowLight;

	btDispatcher dispatcher;
	btConstraintSolver constraintSolver;
	btCollisionConfiguration collisionConfig;
	btSoftBodyWorldInfo softBodyWorldInfo;
	btBroadphaseInterface broadphase;
	btSoftRigidDynamicsWorld dynamicsWorld;
	Contacts contactListener;
	DebugDrawer debugDrawer;

	Model scene;
	Model terrainModel;
	Model tileStartModel;
	ModelInstance tileStartInstance;
	GameObject ground;
	GameObject terrain;
	ModelInstance coords;

	Tile startTile;
	boolean drawTiles = true;

	private final int heightValueRows = 20;
	private final int heightValueCols = 20;
	private final float[] heights = new float[heightValueRows * heightValueCols];
	private float[] vertices;

	final Vector3 lightDir = new Vector3();
	final Color lightColor = Color.WHITE.cpy();

	final float MAX_LIGHT_TIMER = 3f;
	float lightTimer = MAX_LIGHT_TIMER;
	float lightAngle = 0f;

	final Array<GameObject> gameObjects = new Array<>();
	final Array<GameObject> toBeRemoved = new Array<>();
	final ArrayMap<GameObject.Type, GameObject.Builder> gameObjectBuilders = new ArrayMap<>();

	final Array<Disposable> disposables = new Array<>();

//	final float MAX_SPAWN_TIME = 0.1f;
	final float MAX_SPAWN_TIME = 1f;
	float spawnTime = MAX_SPAWN_TIME;

	final float speed = 160f;
//	final float speed = 60f;
	float angle = 0f;

	BitmapFont font;
	Texture texture;
	Model bed;
	ModelInstance bedInstance;

	boolean doDrop = false;

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
		Gdx.app.log("Bullet", "Version = " + LinearMath.btGetVersion());

		modelBatch = new ModelBatch();
		shadowBatch = new ModelBatch(new DepthShaderProvider());
		debugModelBatch = new ModelBatch(new DefaultShaderProvider() {
			@Override
			protected Shader createShader(Renderable renderable) {
				return new WireframeShader(renderable, config);
			}
		});
		spriteBatch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();

		lightDir.set(-1f, -0.8f, -0.2f);
		lightColor.set(0.8f, 0.8f, 0.8f, 1f);
		ambientLightAttrib = ColorAttribute.createAmbientLight(0.3f, 0.3f, 0.3f, 1f);
		directionalShadowLight = new DirectionalShadowLight(4096, 4096,
				100f, 100f, 1f, 100f);
		directionalShadowLight.set(lightColor.r, lightColor.g, lightColor.b, lightDir.x, lightDir.y, lightDir.z);

		env = new Environment();
		env.set(ambientLightAttrib);
		env.add(directionalShadowLight);
		env.shadowMap = directionalShadowLight;

		camera = new PerspectiveCamera(Config.fov, Config.width, Config.height);
		camera.position.set(15f, 10f, 15f);
		camera.lookAt(0f, 4f, 0f);
		camera.update();

		camController = new CameraInputController(camera);
		Gdx.input.setInputProcessor(camController);

		collisionConfig = new btSoftBodyRigidBodyCollisionConfiguration();
		dispatcher = new btCollisionDispatcher(collisionConfig);
		broadphase = new btDbvtBroadphase();
		constraintSolver = new btSequentialImpulseConstraintSolver();
		contactListener = new Contacts();
		softBodyWorldInfo = new btSoftBodyWorldInfo();
		softBodyWorldInfo.setDispatcher(dispatcher);
		softBodyWorldInfo.setBroadphase(broadphase);
		softBodyWorldInfo.getSparsesdf().Initialize();
		softBodyWorldInfo.setGravity(new btVector3(0f, -9.8f, 0f));

		debugDrawer = new DebugDrawer();
		debugDrawer.setSpriteBatch(spriteBatch);
		debugDrawer.setShapeRenderer(shapeRenderer);
		debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_DrawWireframe | btIDebugDraw.DebugDrawModes.DBG_DrawContactPoints);

		dynamicsWorld = new btSoftRigidDynamicsWorld(dispatcher, broadphase, constraintSolver, collisionConfig);
		dynamicsWorld.setDebugDrawer(debugDrawer);
		// TODO - set gravity here and in softBodyWorldInfo, or just one place?
		dynamicsWorld.setGravity(new Vector3(0f, -9.8f, 0f));

		font = new BitmapFont();
		texture = new Texture(Gdx.files.internal("prototype-grid-orange.png"), true);

		createScene();

		disposables.addAll(
				  broadphase
				, dispatcher
				, collisionConfig
				, constraintSolver
				, debugDrawer
				, contactListener
				, softBodyWorldInfo
				, font
				, texture
				, scene
				, terrainModel
				, modelBatch
				, shadowBatch
				, debugModelBatch
				, spriteBatch
				, shapeRenderer
				, directionalShadowLight
				// TODO - this causes a crash, not a huge deal since it happens when the application is closing
				//  but sometime when feeling adventurous, build a bullet dll with debug symbols and go to town:
				//  https://libgdx.com/wiki/extensions/physics/bullet/bullet-wrapper-debugging
//				, dynamicsWorld
		);
	}

	public void update() {
		var delta = Math.min(1f / 30f, Gdx.graphics.getDeltaTime());

		if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
			Gdx.app.exit();
		}
		if (Gdx.input.isKeyJustPressed(Keys.SPACE)) {
			doDrop = !doDrop;
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
//		ground.transform.setTranslation(0, 0f + MathUtils.sinDeg(angle) * 3f, 0f);

		// move the light
		var radius = 2f;
		var speed = 50f;
		lightAngle += delta * speed;
		lightDir.set(
				radius * MathUtils.cosDeg(lightAngle),
				-1f,
				radius * MathUtils.sinDeg(lightAngle)
		).nor();
		directionalShadowLight.setDirection(lightDir);

		lightTimer -= delta;
		if (lightTimer <= 0f) {
			lightTimer = MAX_LIGHT_TIMER;
			lightColor.set(MathUtils.random(0.5f, 1f), MathUtils.random(0.5f, 1f), MathUtils.random(0.5f, 1f), 1f);
			directionalShadowLight.setColor(lightColor);
		}

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
			debugModelBatch.render(startTile.instance, env);
			debugModelBatch.render(straightTile.instance, env);
			debugModelBatch.end();
		} else {
			// first draw the shadows
			directionalShadowLight.begin(Vector3.Zero, camera.direction);
			{
				shadowBatch.begin(directionalShadowLight.getCamera());
				shadowBatch.render(gameObjects);
				// TODO - the Tile instances break here due to a missing DiffuseAttribute?
				shadowBatch.end();
			}
			directionalShadowLight.end();

			// then draw the world normally
			modelBatch.begin(camera);
			modelBatch.render(coords, env);
			modelBatch.render(gameObjects, env);
//			modelBatch.render(bedInstance, env);
			for (var tile : tiles) {
				modelBatch.render(tile.instance, env);
			}
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
		disposables.forEach(Disposable::dispose);
		disposables.clear();

		gameObjects.forEach(GameObject::dispose);
		gameObjectBuilders.values().forEach(GameObject.Builder::dispose);

		gameObjects.clear();
		gameObjectBuilders.clear();

		tiles.forEach(Tile::dispose);
		tiles.clear();

		super.dispose();
	}

	// ------------------------------------------------------------------------
	// Implementation Methods
	// ------------------------------------------------------------------------

	private final float GROUND_SIZE = 50f;

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
			// create a ground plane with a texture repeated across it's surface
			builder.node().id = GROUND.name();
			var diffuseTextureAttribute = TextureAttribute.createDiffuse(texture);
			diffuseTextureAttribute.textureDescription.uWrap = Texture.TextureWrap.Repeat;
			diffuseTextureAttribute.textureDescription.vWrap = Texture.TextureWrap.Repeat;
			diffuseTextureAttribute.textureDescription.minFilter = Texture.TextureFilter.MipMapLinearLinear;
			diffuseTextureAttribute.textureDescription.magFilter = Texture.TextureFilter.MipMapLinearLinear;
			meshPartBuilder = builder.part(GROUND.name(), GL20.GL_TRIANGLES, attribs | Usage.TextureCoordinates,
					new Material(
							  ColorAttribute.createDiffuse(Color.WHITE)
							, ColorAttribute.createSpecular(Color.WHITE)
							, FloatAttribute.createShininess(16f)
							, diffuseTextureAttribute
					)
			);
			meshPartBuilder.rect(
					new MeshPartBuilder.VertexInfo().setPos(-GROUND_SIZE / 2, 0f, -GROUND_SIZE / 2).setNor(Vector3.Y).setCol(Color.WHITE).setUV(0, 0),
					new MeshPartBuilder.VertexInfo().setPos(-GROUND_SIZE / 2, 0f,  GROUND_SIZE / 2).setNor(Vector3.Y).setCol(Color.WHITE).setUV(0, 10),
					new MeshPartBuilder.VertexInfo().setPos( GROUND_SIZE / 2, 0f,  GROUND_SIZE / 2).setNor(Vector3.Y).setCol(Color.WHITE).setUV( 10, 10),
					new MeshPartBuilder.VertexInfo().setPos( GROUND_SIZE / 2, 0f, -GROUND_SIZE / 2).setNor(Vector3.Y).setCol(Color.WHITE).setUV( 10, 0)
			);

			builder.node().id = SPHERE.name();
			meshPartBuilder = builder.part(SPHERE.name(), GL20.GL_TRIANGLES, attribs, new Material(ColorAttribute.createDiffuse(Color.GREEN)));
			SphereShapeBuilder.build(meshPartBuilder, 1.0f, 1.0f, 1.0f, 10, 10);

			builder.node().id = BOX.name();
			meshPartBuilder = builder.part(BOX.name(), GL20.GL_TRIANGLES, attribs, new Material(ColorAttribute.createDiffuse(Color.BLUE)));
			BoxShapeBuilder.build(meshPartBuilder, 1f, 1f, 1f);

			builder.node().id = CONE.name();
			meshPartBuilder = builder.part(CONE.name(), GL20.GL_TRIANGLES, attribs, new Material(ColorAttribute.createDiffuse(Color.YELLOW)));
			ConeShapeBuilder.build(meshPartBuilder, 1f, 2f, 1f, 10);

			builder.node().id = CAPSULE.name();
			meshPartBuilder = builder.part(CAPSULE.name(), GL20.GL_TRIANGLES, attribs, new Material(ColorAttribute.createDiffuse(Color.CYAN)));
			CapsuleShapeBuilder.build(meshPartBuilder, 0.5f, 1f, 10);

			builder.node().id = CYLINDER.name();
			meshPartBuilder = builder.part(CYLINDER.name(), GL20.GL_TRIANGLES, attribs, new Material(ColorAttribute.createDiffuse(Color.MAGENTA)));
			CylinderShapeBuilder.build(meshPartBuilder, 1f, 1f, 1f, 10);

			builder.node().id = COORDS.name();
			float axisLength = 10f;
			float capLength = 0.1f;
			float stemThickness = 0.2f;
			int divisions = 6;
			var coordMaterial = new Material(ColorAttribute.createDiffuse(Color.WHITE));
			meshPartBuilder = builder.part(COORDS.name(), GL20.GL_TRIANGLES, attribs, coordMaterial);
			meshPartBuilder.setColor(Color.WHITE);
			SphereShapeBuilder.build(meshPartBuilder, 0.1f, 0.1f, 0.1f, 10, 10);
			meshPartBuilder.setColor(Color.RED);
			ArrowShapeBuilder.build(meshPartBuilder, 0, 0, 0, axisLength, 0, 0, capLength, stemThickness, divisions);
			meshPartBuilder.setColor(Color.GREEN);
			ArrowShapeBuilder.build(meshPartBuilder, 0, 0, 0, 0, axisLength, 0, capLength, stemThickness, divisions);
			meshPartBuilder.setColor(Color.BLUE);
			ArrowShapeBuilder.build(meshPartBuilder, 0, 0, 0, 0, 0, axisLength, capLength, stemThickness, divisions);
		}
		scene = builder.end();

		coords = new ModelInstance(scene, COORDS.name());

		var loader = new G3dModelLoader(new UBJsonReader());

		bed = loader.loadModel(Gdx.files.internal("bed.g3db"));
		bedInstance = new ModelInstance(bed);
		bedInstance.transform.trn(3f, 3f, 3f);
		disposables.add(bed);

		tileStartModel = loader.loadModel(Gdx.files.internal("start.g3db"));
		tileStartInstance = new ModelInstance(tileStartModel);
		tileStartInstance.transform.trn(5f, 0f, 5f);

		createGameObjectBuilders();

		createTiles();
	}

	private void createGameObjectBuilders() {
		// TODO - this duplicates size parameters from ModelBuilder setup in createScene(), easy to get wrong so centralize
		gameObjectBuilders.put(GROUND,   new GameObject.Builder(0f, scene, GROUND.name(),   new btBox2dShape(new Vector3(GROUND_SIZE / 2, 0f, GROUND_SIZE / 2))));
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

		var shape = new btBvhTriangleMeshShape(tileStartModel.meshParts);
		gameObjectBuilders.put(MESH, new GameObject.Builder(0f, tileStartModel, MESH.name(), shape));

		createGameObjects();
	}

	GameObject start;
	private void createGameObjects() {
		/*
		 * What's all needed to create a minigolf 'tile':
		 * - a model
		 *   - exported from blender as fbx either at 0.01 scale with no scaling on the associated collision shape
		 *   - or exported at 0.1 scale and have to scale the collision shape by 10x (not sure why btBvhTriangleMeshShape creates at a 0.01x scale by default)
		 *   - NOTE - probably easiest to export at 0.01 and not have to scale the collision shape at all, plus things should work on a unit scale for things like translation
		 * - the model instance to draw (currently a part of GameObject)
		 * - add to collision flags: CF_KINEMATIC_OBJECT
		 * - set contactcallback params (*_FLAG, 0)
		 * - set mass to 0f
		 * - no motionState instance (or at least not the same one as 'normal' gameobjects)
		 * - activation state set to DISABLE_DEACTIVATION
		 * - tile models are centered on origin (except for Y), so each tile on the playfield covers x+/-0.5f,z+/-0.5f unless we shift them by half units
		 */



//		start = gameObjectBuilders.get(MESH).build();
//		{
//			start.rigidBody.setCollisionFlags(start.rigidBody.getCollisionFlags()
//					| btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT);
//			start.rigidBody.setContactCallbackFlag(GROUND_FLAG);
//			start.rigidBody.setContactCallbackFilter(0);
//			// NOTE - since this is moved manually the rigid body's activation state shouldn't be managed by Bullet
//			start.rigidBody.setActivationState(Collision.DISABLE_DEACTIVATION);
//			// TODO - can use this to adjust the collision shape's scale to match the g3db model's scaling
//			start.rigidBody.getCollisionShape().setLocalScaling(new Vector3(10f, 10f, 10f));
//			// NOTE - motion state changes the world transform, so if we want to manually adjust it to fit, then motion state has to be disabled (so it'll only work for fixed shapes)
//			start.rigidBody.setMotionState(null);
//			var transform = start.rigidBody.getWorldTransform();
//			transform.rotate(Vector3.X, -90f);
//			transform.trn(5f, 0f, 5f);
//			start.rigidBody.setWorldTransform(transform);
//			// have to translate the render component too
//			start.transform.trn(5f, 0f, 5f);
//		}
//		gameObjects.add(start);
//		dynamicsWorld.addRigidBody(start.rigidBody);

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

//		terrain = gameObjectBuilders.get(TERRAIN).build();
//		{
//			terrain.rigidBody.setCollisionFlags(terrain.rigidBody.getCollisionFlags()
//					| btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT);
//			terrain.rigidBody.setContactCallbackFlag(GROUND_FLAG);
//			terrain.rigidBody.setContactCallbackFilter(0);
//			// NOTE - since this is moved manually the rigid body's activation state shouldn't be managed by Bullet
//			terrain.rigidBody.setActivationState(Collision.DISABLE_DEACTIVATION);
//			terrain.transform.rotate(0f, 1f, 0f, 180f);
//		}
//		gameObjects.add(terrain);
//		dynamicsWorld.addRigidBody(terrain.rigidBody);

//		int numStartingObjects = 1;
//		for (int i = 0; i < numStartingObjects; i++) {
//			spawnObject();
//		}

		createSoftBodyObjectTEST();
	}

	private Model startModel;
	private Model straightModel;
	private Tile straightTile;
	private Array<Tile> tiles = new Array<>();
	private void createTiles() {
		var loader = new G3dModelLoader(new UBJsonReader());
		startModel = loader.loadModel(Gdx.files.internal("tile-start.g3db"));

		var startTile = new Tile(startModel, "tmpParent", 0, -2);
		tiles.add(startTile);
		dynamicsWorld.addRigidBody(startTile.body);

		straightModel = loader.loadModel(Gdx.files.internal("straight.g3db"));
		for (int i = 0; i < 4; i++) {
			var straightTile = new Tile(straightModel, "tmpParent", 0, startTile.coord.z() + 1 + i);
			tiles.add(straightTile);
			dynamicsWorld.addRigidBody(straightTile.body);
		}
	}

	private btSoftBody softBody;
	private void createSoftBodyObjectTEST() {

//		var xyz0 = new Vector3(-10f, 8f, -10f);
//		var xyz1 = new Vector3(10f, 8f, 10f);
//
//		var corner00 = new Vector3(xyz0);
//		var corner11 = new Vector3(xyz1);
//		var corner01 = new Vector3(xyz0.x, xyz0.y, xyz1.z);
//		var corner10 = new Vector3(xyz1.x, xyz1.y, xyz0.z);
//
//		softBody = btSoftBodyHelpers.CreatePatch(softBodyWorldInfo, corner00, corner10, corner01, corner11, 10, 10, 10, false);
//		softBody.takeOwnership();
//		softBody.setTotalMass(100f);
//		softBody.setContactCallbackFlag(SOFT_FLAG);
//		softBody.setContactCallbackFilter(0);
//		dynamicsWorld.addSoftBody(softBody);
//
//		softBody.setMass(0, 0f);
//		softBody.setMass(9, 0f);
//		softBody.setMass(90, 0f);
//		softBody.setMass(99, 0f);

		// TODO - works, but crashes when it interacts with 'terrain' body
	}

	private GameObject spawnObject() {
		return spawnObject(
				MathUtils.random(-2.5f, 2.5f), MathUtils.random(15, 20f), MathUtils.random(-2.5f, 2.5f),
				MathUtils.random(360f), MathUtils.random(360f), MathUtils.random(360f)
		);
	}

	private GameObject spawnObject(Vector3 position, Vector3 angles) {
		return spawnObject(position.x, position.y, position.z, angles.x, angles.y, angles.z);
	}

	private GameObject spawnObject(float posX, float posY, float posZ, float angleX, float angleY, float angleZ) {
		// TODO - add to disposables
		var object = gameObjectBuilders.get(GameObject.Type.random()).build();
		{
			object.transform.setFromEulerAngles(angleX, angleY, angleZ);
			object.transform.trn(posX, posY, posZ);
			object.rigidBody.proceedToTransform(object.transform);
			object.rigidBody.setUserValue(gameObjects.size);
			object.rigidBody.setCollisionFlags(object.rigidBody.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
			object.rigidBody.setContactCallbackFlag(OBJECT_FLAG);
			object.rigidBody.setContactCallbackFilter(SOFT_FLAG);
		}
		gameObjects.add(object);
		dynamicsWorld.addRigidBody(object.rigidBody);
		return object;
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