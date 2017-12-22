package me.kalmanbncz.nd.ar.entity;

import java.util.Random;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.TimeUtils;
import me.kalmanbncz.nd.ar.BlueExplosion;
import me.kalmanbncz.nd.ar.BulletHit;
import me.kalmanbncz.nd.ar.Physics;
import me.kalmanbncz.nd.ar.ProgressBar;
import me.kalmanbncz.nd.ar.Renderer;
import me.kalmanbncz.nd.util.Log;

/**
 * The enemy class which holds all the data for navigation randomly or with a target through the 3d space
 * Created by Kali on 2/26/2015.
 */
public class EnemyInstance extends ModelInstance implements AnimationController.AnimationListener {

    private static final float[] zerofloat = new float[]{0, 0, 1, 0};

    public static Array<EnemyInstance> list = new Array<>();

    public static Array<Integer> destroyQueue = new Array<>();

    public static IntMap<EnemyInstance> idMap = new IntMap<>();

    public static Array<Integer> usedIDs = new Array<>();

    public static Model enemyModel;

    protected static Quaternion q = new Quaternion();

    protected static Matrix4 mtx = new Matrix4();

    protected static Matrix4 modelmtx = new Matrix4();

    private static Vector3 axis = new Vector3();

    private static float[] angleholder = new float[]{0, 0, 0, 0};

    private static int nextEnemyInstanceId;

    private static long minJumpInterval = 250;

    private static float radius;

    public float[] arrowPosition = new float[4];

    //    public Vector3 bbCorner000 = new Vector3();
//    public Vector3 bbCorner100 = new Vector3();
//    public Vector3 bbCorner010 = new Vector3();
//    public Vector3 bbCorner001 = new Vector3();
//    public Vector3 bbCorner110 = new Vector3();
//    public Vector3 bbCorner101 = new Vector3();
//    public Vector3 bbCorner011 = new Vector3();
//    public Vector3 bbCorner111 = new Vector3();
    public AnimationController animController;

    public Vector3 target = new Vector3(0, 0, 0);

    public BoundingBox modelBoundingBox;

    public boolean draw;

    public boolean notInFrustum;

    public int id = -1;

    public float health = 100f;

    public Movement movement;

    public boolean onGround;

    public float distFromGround;

    public boolean destroyed;

    public btCollisionObject body;

    public Vector3 bodyOffset = new Vector3();

    /**
     * height, width, depth dimensions of the entity
     */
    public Vector3 dimen = new Vector3();

    public boolean isPlayer;

    public btCapsuleShape entityShape;

    protected Quaternion rotation = new Quaternion();

    /**
     * the Physics class collision callbacks will set collision position change, which is processed during update
     */
    protected Vector3 collisionPositionChanges = new Vector3();

    protected int collisionPositionChangeCount = 0;

    private Vector3 tmp = new Vector3();

    private Vector3 tmp2 = new Vector3();

    private Vector3 tmp3 = new Vector3();

    private Vector3 modelTopCoords = new Vector3();

    private float speed = (new Random().nextInt(5) + 4) / 10f;

    private Vector3 pos = new Vector3();

    private Sprite hudIndicator;

    private ProgressBar lifeBar;

    private long lastSetDestTime;

    private int nextDestInterval = 10000;

    private long lastJumpTime = 0;

    private int ticksPerShot = 6;

    private int tickCountdown;

    private boolean shooting;

    private float aimError = 0.025f;

    private Vector3 topCoords = new Vector3();

    public EnemyInstance(Model model, boolean isPlayer) {
        super(model);
        this.isPlayer = isPlayer;
        if (!isPlayer) {
            synchronized (EnemyInstance.list) {
                list.add(this);
            }

            movement = new FlyingMovement(id);
            movement.setAffectedByGravity(false);
            movement.setAccelRate(1f);
            movement.setMaxHorizontalSpeed(1f);
            movement.setMaxVerticalSpeed(1f);
            movement.setBraking(1f);
//            movement = new FlyingMovement(id);
//            movement.setAffectedByGravity(false);
//            movement.setAccelRate(0f);
//            movement.setMaxHorizontalSpeed(0f);
//            movement.setMaxVerticalSpeed(0f);
//            movement.setBraking(1f);
            this.setDestination(Renderer.playerPosition);

        } else {
            draw = true;
            movement = new FlyingMovement(id);
            movement.setAffectedByGravity(false);
            movement.setAccelRate(0f);
            movement.setMaxHorizontalSpeed(0f);
            movement.setMaxVerticalSpeed(0f);
            movement.setBraking(1f);
            transform.setToTranslation(0, 0, 0);
        }
//        lifeBar = new ProgressBar(0,100,10,false,new Skin());

    }

    public static EnemyInstance newInstance(int x, int y, int z, int scale, Model model, boolean isPlayer) {

        final EnemyInstance instance = new EnemyInstance(model, isPlayer);

        instance.transform.setToTranslation(x, y, z);
        instance.setPosition(x, y, z);
        instance.transform.scale(scale, scale, scale);
        if (!isPlayer) {
            instance.animController = new AnimationController(instance);
            instance.animController.setAnimation("Default Take", -1);
            instance.modelBoundingBox = new BoundingBox();
            instance.calculateBoundingBox(instance.modelBoundingBox);
            instance.dimen.set(instance.modelBoundingBox.getWidth(), instance.modelBoundingBox.getHeight(), instance.modelBoundingBox.getDepth());
//        tmp.set(instance.dimen).scl(0.5f);
            radius = 0.36f * (float) Math.sqrt((instance.dimen.x * instance.dimen.x) + (instance.dimen.z * instance.dimen.z));
            ;
            instance.entityShape = new btCapsuleShape(radius, instance.dimen.y * 0.1f);
            instance.body = Physics.inst.createCapsuleObject(instance.entityShape);
            instance.bodyOffset.set(0f, -0.8f, 0f);
            Physics.applyEnemyInstanceCollisionFlags(instance.body);
            instance.body.userData = instance;

            Physics.inst.addStaticGeometryToWorld(instance.body);
            instance.body.setWorldTransform(instance.transform);
        } else {
//        instance.getLifeBar().setRange(0,100);
//        instance.getLifeBar().setValue(instance.health);
            instance.modelBoundingBox = new BoundingBox();
            instance.calculateBoundingBox(instance.modelBoundingBox);
            instance.dimen.set(instance.modelBoundingBox.getWidth(), instance.modelBoundingBox.getHeight(), instance.modelBoundingBox.getDepth());
//        tmp.set(instance.dimen).scl(0.5f);
            radius = 0.36f * (float) Math.sqrt((instance.dimen.x * instance.dimen.x) + (instance.dimen.z * instance.dimen.z));
            ;
            instance.entityShape = new btCapsuleShape(radius, instance.dimen.y * 0.1f);
            instance.body = Physics.inst.createCapsuleObject(instance.entityShape);
            instance.bodyOffset.set(0f, -0.8f, 0f);
            Physics.applyEnemyInstanceCollisionFlags(instance.body);
            instance.body.userData = instance;

            Physics.inst.addEnemyInstanceToWorld(instance.body);
            instance.body.setWorldTransform(instance.transform);
        }
        return instance;
    }


    public static void updateAll(float timeStep) {
        for (int i = 0; i < EnemyInstance.list.size; i++) {
            EnemyInstance ent = EnemyInstance.list.get(i);
            ent.update(timeStep);
        }
        processDestroyQueue();
    }

    public static void processDestroyQueue() {
        synchronized (destroyQueue) {
            for (int id : destroyQueue) {
                synchronized (EnemyInstance.list) {
                    for (EnemyInstance ent : list) {
                        if (ent.id == id) {
                            ent.removeFromGame();
                            // id should be unique, unless something is broken
                            break;
                        }
                    }
                }
            }
            destroyQueue.clear();
        }
    }

    void update(float timeStep) {
        animController.update(timeStep);
        boolean testSimpleAI = true;
        handleCollisions();
        // when the ray went the full length and did not hit the ground, NaN is the return value
        onGround = false;
        float embedThreshold = 0f;
        if (!Float.isNaN(distFromGround)) {
            Vector3 vel = getVelocity();
            if (distFromGround < 0.1f) {
                adjustPosition(tmp.set(0f, -distFromGround, 0f));
                if (vel.y < 0f) vel.y = 0f;
                onGround = true;
            }
            if (distFromGround < embedThreshold) {
                // penetrating into the ground
                getPosition().y += -distFromGround;
            } else if (distFromGround > 0f) {
                Vector3 velocity = getVelocity();
                // cap velocity to distance from ground
                if (velocity.y < 0 && distFromGround - velocity.y <= 0f) {
                    velocity.y = -distFromGround;
                    Log.log("cap velocity: " + velocity.y);
                }
            }
        }
        movement.update(timeStep, onGround);
        updateTransforms();
        if (testSimpleAI) {
//			if (Main.isClient() && Main.inst.client.player != null) {
//				// look at the player entity (billboard behaviour, always face the camera)
            lookAt(Renderer.playerPosition);
//			}
            long now = TimeUtils.millis();
            movement.cancelDestinationAtThreshold(3f);
            if ((now - lastSetDestTime) >= nextDestInterval) {
                float x = MathUtils.random(0f, 100f);
                float y = 0f;
                float z = MathUtils.random(0f, 100f);
                setDestination(tmp.set(x, y, z));
                lastSetDestTime = now;
                nextDestInterval = MathUtils.random(5000, 15000);
            }
        }
        if (shooting) {
            if (tickCountdown <= 0) {
                shoot();
                tickCountdown = ticksPerShot;
            }
        }
        tickCountdown--;
    }

    public void updateTransforms() {
        // physics body transform
        q.setEulerAngles(rotation.getYaw(), rotation.getPitch(), rotation.getRoll()); // physics bodies only care about yaw
        mtx.set(q);
        modelmtx.set(q);
        mtx.setTranslation(tmp.set(getPosition()).add(bodyOffset));
        modelmtx.setTranslation(tmp.set(getPosition()));//.add(bodyOffset)
        if (body != null) {
            body.setWorldTransform(mtx);
        }
        transform.set(modelmtx);
    }

    private Vector3 getTempPoint() {
        Vector3 vector = new Vector3();
        vector.x = new Random().nextInt(13) + target.x;
        vector.y = new Random().nextInt(3) + target.y;
        vector.z = new Random().nextInt(13) + target.z;
        return vector;
    }

    @Override
    public void onEnd(AnimationController.AnimationDesc animation) {

    }

    @Override
    public void onLoop(AnimationController.AnimationDesc animation) {

    }

    public BoundingBox getBoundingBox() {
        return modelBoundingBox;
    }

    public boolean isInCrosshair(PerspectiveCamera camera) {
        draw = false;
        if (camera.frustum.boundsInFrustum(modelBoundingBox)) {
            draw = true;


//            float xLimit = Math.abs(Math.abs(bbCorner000.x) - Math.abs(bbCorner100.x)) / 2;
//            float yLimit = Math.abs(Math.abs(bbCorner000.y) - Math.abs(bbCorner010.y)) / 2;
//            if (Math.abs(bbCenter.x - Gdx.graphics.getWidth() / 2) < xLimit && Math.abs(bbCenter.y - Gdx.graphics.getHeight() / 2) < yLimit) {
//                me.kalmanbncz.nd.ar.Renderer.currentCRTexture = Renderer.crossHairs.get(1);
////                if (Renderer.firing) {
////                    applyDamage(3);
////                }
////                    if (headshot)
//                return true;
//            }
        }
        return false;
    }

    public Vector3 getModelTopCoords() {
        return topCoords.set(getPosition()).add(0, getRadius(), 0).add(bodyOffset);
    }

    public Sprite getHudIndicator() {
        return hudIndicator;
    }

    public void setHudIndicator(Sprite hudIndicator) {
        this.hudIndicator = hudIndicator;
    }

    // TODO is there a more correct way to handle collision position changes?

    public ProgressBar getLifeBar() {
        return lifeBar;
    }

    /**
     * The way bullet works, there might be multiple collision points when two object collide with each other.
     * Therefore, we take the average of the position change caused by each collision and apply it as the final
     * position change.
     */
    public void handleCollisions() {
        if (collisionPositionChangeCount > 0) {
            collisionPositionChanges.scl(1f / collisionPositionChangeCount);
            collisionPositionChanges.scl(-1f); // subtraction
            adjustPosition(collisionPositionChanges);
            collisionPositionChangeCount = 0;
            collisionPositionChanges.setZero();
        }
    }

    // TODO check this works properly
//    public void faceTowards(Vector3 targ) {
//        float desired = Tools.getAngleFromAtoB(movement.getPosition(), targ, Vector3.Y);
//        rotation.setEulerAngles(-desired, rotation.getPitch(), rotation.getRoll());
//    }


    public void setDestination(Vector3 d) {
        movement.setDestination(d);
    }

    public void setRelativeDestination(Vector3 delta) {
        tmp.set(movement.getPosition()).add(delta);
        setDestination(tmp);
    }

    /**
     * Stops ground units from moving up when looking up.
     * for example, if an entity is rotated to be facing almost straight up,
     * this method relativizes the destination to be "in front of" the entity
     * on the xz (ground) plane
     */
    public void setRelativeDestinationByYaw(Vector3 delta) {
        // TODO what happens if one of the added vectors == Vector3.Y? i.e. straight up or straight down
        relativizeByYaw(delta);
        tmp.set(movement.getPosition()).add(delta);
        setDestination(tmp);
    }

    /**
     * transform vector based on the current rotation, but set pitch to zero, useful for relative directions like "forward" and "back"
     */
    public Vector3 relativizeByYaw(Vector3 v) {
        q.setEulerAngles(rotation.getYaw(), 0f, 0f);
        q.transform(v);
        return v;
    }

    /**
     * transform vector by current rotation, makes vector relative to current facing
     */
    public Vector3 relativize(Vector3 v) {
        rotation.transform(v);
        return v;
    }

    public void setPosition(float x, float y, float z) {
        setPosition(tmp.set(x, y, z));
    }

    public void adjustPosition(Vector3 delta) {
        movement.getPosition().add(delta);
    }

    /**
     * combines all collision position changes, which are averaged when processed during update
     */
    public void addCollisionPositionChange(Vector3 posDelta) {
        collisionPositionChangeCount++;
        collisionPositionChanges.add(posDelta);
    }

    public Movement getMovement() {
        return movement;
    }

    public Vector3 getPosition() {
        return movement.getPosition();
    }

    public void setPosition(Vector3 pos) {
        if (pos == null) throw new NullPointerException();
        if (movement == null) throw new NullPointerException();
        movement.setPosition(pos);
    }

    public Vector3 getVelocity() {
        return movement.getVelocity();
    }

    public void setVelocity(Vector3 vel) {
        movement.getVelocity().set(vel);
    }

    public Quaternion getRotation() {
        return rotation;
    }

    public void setRotation(Quaternion newRot) {
        rotation.set(newRot);
    }

    public void adjustVelocity(Vector3 delta) {
        movement.getVelocity().add(delta);
    }

    /**
     * adjust velocity based on relative directions. i.e. Vector3.Z == forward, (0, 1, 1) == forward-up
     */
    public void adjustVelocityRelativeByYaw(Vector3 delta) {
        adjustVelocity(relativizeByYaw(delta));
    }

    public void setYawPitchRoll(float y, float p, float r) {
        getRotation().setEulerAngles(y, p, r);
    }

    public void setYawPitchRoll(Vector3 rot) {
        getRotation().setEulerAngles(rot.x, rot.y, rot.z);
    }

    public void lookAt(Vector3 pos) {
        tmp.set(pos).sub(getPosition());
        q.setFromCross(Vector3.Z, tmp.nor());
        setYawPitchRoll(q.getYaw(), getPitch(), getRoll());
    }

    public float getYaw() {
        return rotation.getYaw();
    }

    public void setYaw(float amt) {
        rotation.setEulerAngles(amt, rotation.getPitch(), rotation.getRoll());
    }

    public void adjustYaw(float amt) {
        float yaw = getYaw();
        yaw += amt;
        setYaw(yaw);
    }

    public float getPitch() {
        return rotation.getPitch();
    }

    public void setPitch(float amt) {
        rotation.setEulerAngles(rotation.getYaw(), amt, rotation.getRoll());
    }

    public void adjustPitch(float amt) {
        float pitch = getPitch();
        // avoid gimbal lock
        // technically could use Quaternions for free rotation, but not necessary for FPS
        pitch = MathUtils.clamp(pitch + amt, -89f, 89f);
        setPitch(pitch);
    }

    public float getRoll() {
        return rotation.getRoll();
    }

    public void setRoll(float amt) {
        rotation.setEulerAngles(rotation.getYaw(), rotation.getPitch(), amt);
    }

    public void adjustRoll(float amt) {
        float roll = getRoll();
        roll += amt;
        setRoll(roll);
    }

    public Vector3 getDimensions() {
        return dimen;
    }

    public boolean isFlyingEnemyInstance() {
        return movement instanceof FlyingMovement;
    }

    public void destroy() {
        if (destroyed) return;
        destroyed = true;
        synchronized (destroyQueue) {
            removeFromGame();
            new BlueExplosion(getPosition());
        }
    }

    protected void removeFromGame() {
        synchronized (EnemyInstance.list) {
            list.removeValue(this, true);
        }
        Physics.inst.removeBody(body);
    }

    public void applyDamage(float dmg) {
        //Log.debug("EnemyInstance[" + id + "] took damage: " + dmg);
        health -= dmg;
        if (health <= 0f && !isPlayer) {
            destroy();
        }
//        lifeBar.setValue(health);
    }

    public float getHeight() {
        return dimen.y;
    }

    public float getWidth() {
        return dimen.x;
    }

    public float getDepth() {
        return dimen.z;
    }

    // TODO make this more accurate
    public float getRadius() {
        return radius;
    }

    public Vector3 getForwardFacing(Vector3 storage) {
        storage.set(Vector3.Z);
        relativize(storage);
        return storage;
    }

    public void jump() {
        if (onGround) {
            long now = System.currentTimeMillis();
            if ((now - lastJumpTime) < minJumpInterval) {
                return;
            }
            lastJumpTime = now;
            float jumpStrength = 0.35f;
            adjustVelocity(tmp.set(0f, jumpStrength, 0f));
        }
    }

    public void startShoot() {
        shooting = true;
    }

    public void stopShoot() {
        shooting = false;
    }

    public void shoot() {
        // self pos = ray start
        tmp.set(getPosition());
        // store forward vector in tmp2 and keep for processing hit result
        // also add aim error
        float xErr = MathUtils.random(-aimError, aimError);
        float yErr = MathUtils.random(-aimError, aimError);
        tmp2.set(Vector3.Z).scl(-1).add(xErr, yErr, 0f);
        relativize(tmp2);
        float rayLen = 1000f;
        // get forward facing and apply aim error
        tmp3.set(tmp2).scl(rayLen).add(tmp);
//        Physics.inst.castRay(tmp, tmp3);
        Log.log(tmp3);
        if (isPlayer) {
            Physics.inst.castRayStaticOnly(tmp, tmp3);
        } else {
            Physics.inst.castRayDinamicOnly(tmp, tmp3);
        }
        Physics.RaycastReport ray = Physics.inst.raycastReport;
        if (ray.hit) {
            // position + (forward vector * hitDistance) = hit location
            tmp2.scl(ray.hitDistance).add(tmp);
            new BulletHit(tmp2, getRadius());
            Log.log("[ EnemyInstance hit at " + tmp2);
        }
    }

    public void shoot(Quaternion direction) {
        // self pos = ray start
        tmp.set(getPosition());
        // store forward vector in tmp2 and keep for processing hit result
        // also add aim error
        float xErr = MathUtils.random(-aimError, aimError);
        float yErr = MathUtils.random(-aimError, aimError);
        tmp2.set(Vector3.Z);//.add(xErr, yErr, 0f);
        direction.transform(tmp2);
        float rayLen = 1000f;
        // get forward facing and apply aim error
        tmp3.set(tmp2).scl(rayLen).add(tmp);
        if (Renderer.getInstance().getCamera().frustum.pointInFrustum(tmp3)) {
            Log.log("[ target in frustum");
        }
        Log.log(tmp3);
        Physics.inst.castRay(tmp, tmp3);
        Physics.RaycastReport ray = Physics.inst.raycastReport;
        if (ray.hit) {
            // position + (forward vector * hitDistance) = hit location
            tmp2.scl(ray.hitDistance).add(tmp);
            new BulletHit(tmp2, getRadius());
            if (list.size > 0) {
                list.get(list.size - 1).applyDamage(10);
            }
            Log.log("[ EnemyInstance hit at " + tmp2);
        }
    }
//    public void shoot(Vector3 target) {
//        // self pos = ray start
//        tmp.set(getPosition());
//        // store forward vector in tmp2 and keep for processing hit result
//        // also add aim error
////        float xErr = MathUtils.random(-aimError, aimError);
////        float yErr = MathUtils.random(-aimError, aimError);
////        tmp2.set(Vector3.Z).add(xErr, yErr, 0f);
////        relativize(tmp2);
//        float rayLen = 1000f;
//        // get forward facing and apply aim error
////        tmp3.set(tmp2).scl(rayLen).add(tmp);
//        tmp2.set(target);
//        Physics.inst.castRay(tmp, tmp2);
//        Physics.RaycastReport ray = Physics.inst.raycastReport;
//        if (ray.hit) {
//            // position + (forward vector * hitDistance) = hit location
//            tmp2.scl(ray.hitDistance).add(tmp);
//            new BulletHit(list.get(0).getPosition());
//        }
//    }
}
