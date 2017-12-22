package me.kalmanbncz.nd.ar;

import java.util.ArrayList;
import java.util.Random;
import android.app.Activity;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.TimeUtils;
import me.kalmanbncz.nd.ar.entity.EnemyInstance;
import me.kalmanbncz.nd.sensor.SensorFusion;
import me.kalmanbncz.nd.util.Log;

public class Renderer extends Game implements View.OnTouchListener {

    public static final Vector3 playerPosition = new Vector3(0, 0, 0);

    private static final int FRAME_COLS = 3;

    public static ArrayList<Texture> crossHairs = new ArrayList<>();

    public static Texture currentCRTexture;

    public static boolean firing;

    public static boolean debug = false;

    public static int physicsTime;

    private static Renderer instance;

    private final Context context;

    private final ProgressBar progressBar;

    public ModelBatch modelBatch;

    public Environment environment;

    public EnemyInstance player;

    private Animation shootAnimation;

    private Texture shootTexture;

    private TextureRegion[] shootFrames;

    private TextureRegion currentFrame;

    private PerspectiveCamera camera;

    private OrthographicCamera hudCam;

    private SpriteBatch hudBatch;

    private SensorFusion mSensorFusion;

    private Matrix4 rotationMatrix = new Matrix4();

    private Vector3 indicatorPos = new Vector3();

    private Vector3 instancePos = new Vector3();

    private Sprite cross;

    private Texture dotTexture;

    private int crosshairX;

    private int crosshairY;

    private int crosshairSize;

    private int dotSize;

    private float stateTime;

    private BitmapFont font;

    private Vector3 screenCenter = new Vector3();

    private Quaternion tempQuat = new Quaternion();

    private boolean startedRecreating = true;

    private float accumulatedPhysicsTime;

    private long lastPhysicsTime = -1;

    private boolean needToCreateModels = false;

    public Renderer(Context context, ProgressBar progressBar) {
        instance = this;
        this.context = context;
        this.progressBar = progressBar;
        this.progressBar.setMax(1000);
        mSensorFusion = new SensorFusion(context);
    }

    public synchronized static double calcRotationAngleInDegrees(float x, float y, Vector3 targetPt) {
        double theta = Math.atan2(targetPt.y - y, targetPt.x - x);
        theta += Math.PI / 2.0;
        double angle = Math.toDegrees(theta);
        if (angle < 0) {
            angle += 360;
        }
        return angle;
    }

    public static Renderer getInstance() {
        return instance;
    }

    public synchronized void createModels() {
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);
            }
        });

        Texture indicatorTexture = new Texture(Gdx.files.internal("drawables/arrow_red.png"));
        Random random = new Random();
        if (EnemyInstance.enemyModel == null) {
            EnemyInstance.enemyModel = Assets.manager.get("models/brain-r.g3db", Model.class);
        }
        Particles.inst.createEffects();
        if (player == null) {
            player = EnemyInstance.newInstance(0, 0, 0, 1, EnemyInstance.enemyModel, true);
        }
//        float radius = (float) Math.sqrt((player.dimen.x * player.dimen.x) + (player.dimen.z * player.dimen.z));
//        player.entityShape = new btCapsuleShape(radius, player.dimen.y*2);
//        player.body = Physics.inst.createCapsuleObject(player.entityShape);
//        player.bodyOffset.set(0f, 0.0f, 0f);
//        Physics.applyEnemyInstanceCollisionFlags(player.body);
//        player.body.userData = player;
//
//        Physics.inst.addEnemyInstanceToWorld(player.body);
//        player.body.setWorldTransform(player.transform);
        int x, y, z;
        int num = 5 + random.nextInt(5);
        for (int i = 0; i < num; i++) {//5+random.nextInt(10)
            x = random.nextInt(70) * (random.nextBoolean() ? -1 : 1);
            y = random.nextInt(10);
            z = 20 + random.nextInt(40) * (random.nextBoolean() ? -1 : 1);
            final EnemyInstance instance = EnemyInstance.newInstance(x, y, z, 1, EnemyInstance.enemyModel, false);
            Sprite hudIndicator = new Sprite(indicatorTexture, 0, 0, indicatorTexture.getWidth(), indicatorTexture.getHeight());
            instance.setHudIndicator(hudIndicator);
            progressBar.setProgress(progressBar.getProgress() + progressBar.getMax() / num);
        }
        progressBar.setProgress(progressBar.getMax());
        Log.log("KALI - loaded");
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "Loaded", Toast.LENGTH_LONG).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void create() {
        modelBatch = new ModelBatch() {
            @Override
            public <T extends RenderableProvider> void render(final Iterable<T> renderableProviders) {
                for (final RenderableProvider renderableProvider : renderableProviders) {
                    if (renderableProvider instanceof EnemyInstance && (!((EnemyInstance) renderableProvider).draw) || ((EnemyInstance) renderableProvider).destroyed) {
                        return;
                    }
                    render(renderableProvider);
                }
            }
        };
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));


        camera = new PerspectiveCamera(30, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, 0f, 0f);
        camera.near = 0.05f;
        camera.far = 300f;
        camera.update();
//        Gdx.input.setInputProcessor(new CameraInputController(camera));

        screenCenter.set(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2, 0);
        hudCam = new OrthographicCamera();
        hudCam.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        hudBatch = new SpriteBatch();

        Assets.create();
        Assets.loading = true;

        Log.log("reached initAssets");

        modelBatch = new ModelBatch();
        Pooler.init();
        new Physics();
        new Particles();
        Texture s1 = new Texture(Gdx.files.internal("drawables/cr_stage1.png"));
        Texture s2 = new Texture(Gdx.files.internal("drawables/cr_stage2.png"));
        Texture s3 = new Texture(Gdx.files.internal("drawables/cr_stage3.png"));
        Texture s4 = new Texture(Gdx.files.internal("drawables/cr_stage4.png"));
        Texture s5 = new Texture(Gdx.files.internal("drawables/cr_stage5.png"));
        dotTexture = new Texture(Gdx.files.internal("drawables/ic_picsphere_viewfinder.png"));

        crossHairs.add(s1);
        crossHairs.add(s2);
        crossHairs.add(s3);
        crossHairs.add(s4);
        crossHairs.add(s5);
        cross = new Sprite(s1, 0, 0, s1.getWidth(), s1.getHeight());
        currentCRTexture = s1;
        dotSize = Gdx.graphics.getWidth() / 100;

        crosshairSize = Gdx.graphics.getWidth() / 7;
        crosshairX = Gdx.graphics.getWidth() / 2 - crosshairSize / 2;
        crosshairY = Gdx.graphics.getHeight() / 2 - crosshairSize / 2;
        cross.setX(Gdx.graphics.getWidth() / 2 - s1.getWidth() / 2);
        cross.setY(Gdx.graphics.getHeight() / 2 - s1.getHeight() / 2);

        shootTexture = new Texture(Gdx.files.internal("drawables/beamtexture.png"));
        TextureRegion[][] tmp = TextureRegion.split(shootTexture, shootTexture.getWidth() / FRAME_COLS, shootTexture.getHeight() / FRAME_COLS);
        shootFrames = new TextureRegion[FRAME_COLS * FRAME_COLS];
        int index = 0;
        for (int i = 0; i < FRAME_COLS; i++) {
            for (int j = 0; j < FRAME_COLS; j++) {
                shootFrames[index++] = tmp[i][j];
            }
        }
        shootAnimation = new Animation(0.01f, shootFrames);
        stateTime = 0f;

        font = new BitmapFont();
    }

    @Override
    public void resize(int width, int height) {
//        create();
        camera = new PerspectiveCamera(30, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, 0f, 0f);
        camera.near = 0.05f;
        camera.far = 300f;
        camera.update();

        screenCenter.set(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2, 0);
        hudCam = new OrthographicCamera();
        hudCam.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        cross = new Sprite(crossHairs.get(0), 0, 0, crossHairs.get(0).getWidth(), crossHairs.get(0).getHeight());
        dotSize = Gdx.graphics.getWidth() / 100;

        crosshairSize = Gdx.graphics.getWidth() / 7;
        crosshairX = Gdx.graphics.getWidth() / 2 - crosshairSize / 2;
        crosshairY = Gdx.graphics.getHeight() / 2 - crosshairSize / 2;
        cross.setX(Gdx.graphics.getWidth() / 2 - crossHairs.get(0).getWidth() / 2);
        cross.setY(Gdx.graphics.getHeight() / 2 - crossHairs.get(0).getHeight() / 2);
    }

    public synchronized void render() {
        if (Assets.loading && Assets.manager.update()) {
            createModels();
            Assets.loading = false;
            startedRecreating = false;
        } else if (Assets.loading) {
            progressBar.setProgress((int) (Assets.manager.getProgress() * 1000 + 10));
        }
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        rotationMatrix.set(mSensorFusion.getRotationMatrix());
        camera.rotate(rotationMatrix);

        if (!Assets.loading) {
            player.setRotation(camera.view.getRotation(tempQuat));
            player.updateTransforms();
        }
//        camera.rotate(GameActivity.cameraRot, 0, 1, 0);
        camera.update();
//        camera.rotate(-GameActivity.cameraRot, 0, 1, 0);
        camera.rotate(rotationMatrix.inv());


        if (!Assets.loading) {

//        player.transform.setToRotation(camera.view.getRotation(tempQuat));
//            player.transform.setToRotation(camera.up, camera.direction);
//            player.setRotation(camera.view.getRotation(tempQuat));
            player.setRotation(camera.view.getRotation(tempQuat).conjugate());
            player.updateTransforms();
            modelBatch.begin(camera);
            updateWorld();
            EnemyInstance.updateAll(Gdx.graphics.getDeltaTime());
            BulletHit.updateAll(Gdx.graphics.getDeltaTime());
            BlueExplosion.updateAll(Gdx.graphics.getDeltaTime());
            for (EnemyInstance instance : EnemyInstance.list) {
                instance.isInCrosshair(camera);
            }
            screenCenter.set(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2, 1);
            camera.unproject(screenCenter);
            //player.faceTowards(camera.unproject(screenCenter));
            if (firing) {
//                rotationMatrix.getRotation(tempQuat);
//                player.shoot(tempQuat);
                player.shoot();
            }
            modelBatch.render(EnemyInstance.list);
            if (debug) {
                Physics.inst.debugDraw();
            }
            drawParticleEffects();
            modelBatch.end();
        }
        renderHud(!Assets.loading);
        if (needToCreateModels) {
            needToCreateModels = false;
            startedRecreating = true;
            createModels();
            startedRecreating = false;
        }
        if (EnemyInstance.list.size < 1 && !startedRecreating) {
            needToCreateModels = true;
        }

    }

    public void updateWorld() {
        long start = TimeUtils.millis();
        if (lastPhysicsTime == -1) {
            lastPhysicsTime = start;
            return;
        }
        float delta = (start - lastPhysicsTime) / 1000f;
        lastPhysicsTime = start;
        accumulatedPhysicsTime += delta;
        //Log.debug("physics delta: " + delta);
        //Log.debug("accumualted physics time: " + accumulatedPhysicsTime);
        while (accumulatedPhysicsTime >= Physics.TIME_STEP) {
            Physics.inst.run();
            accumulatedPhysicsTime -= Physics.TIME_STEP;
            //Entity.updateAll(Physics.TIME_SCALE);
        }
        physicsTime = (int) TimeUtils.timeSinceMillis(start);
    }

    @Override
    public void pause() {
        mSensorFusion.onPauseOrStop();
    }

    @Override
    public void resume() {
        mSensorFusion.onResume();
    }

    @Override
    public void dispose() {
        mSensorFusion.onPauseOrStop();
        Assets.dispose();
    }

    private void drawParticleEffects() {
        Particles.inst.system.update();
        Particles.inst.system.begin();
        Particles.inst.system.draw();
        Particles.inst.system.end();
        modelBatch.render(Particles.inst.system);
    }

    private synchronized void renderHud(boolean doneLoading) {
        hudCam.position.set(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2, 0.0f);
        hudCam.update();
        hudBatch.setProjectionMatrix(hudCam.combined);
        hudBatch.begin();
        stateTime += Gdx.graphics.getDeltaTime();
        if (doneLoading) {
            calculateHudArrowPositions();
            for (int i = 0; i < EnemyInstance.list.size; i++) {
                EnemyInstance instance = EnemyInstance.list.get(i);
                if (instance.notInFrustum) {
                    float[] v = EnemyInstance.list.get(i).arrowPosition;
                    Texture t = instance.getHudIndicator().getTexture();
                    instance.getHudIndicator().setPosition(v[0] - t.getWidth() / 2, v[1] - t.getHeight() / 2);
                    instance.getHudIndicator().setRotation(v[3]);
                    instance.getHudIndicator().draw(hudBatch);
                } else {
                    if (debug) {
                        Vector3 topCoords = camera.project(instance.getModelTopCoords());
//                        font.draw(hudBatch,  String.valueOf(instance.getPosition()), instance.bbCenter.x, instance.bbCorner000.y);
                        font.draw(hudBatch, String.valueOf(instance.health), topCoords.x, topCoords.y);

//                        hudBatch.draw(dotTexture, instance.getPosition().x, instance.bbCenter.y, dotSize, dotSize);
//                        instance.getLifeBar().draw(hudBatch,1);
//                        hudBatch.draw(dotTexture, instance.bbCorner000.x, instance.bbCorner000.y, dotSize, dotSize);
//                        hudBatch.draw(dotTexture, instance.bbCorner100.x, instance.bbCorner100.y, dotSize, dotSize);
//                        hudBatch.draw(dotTexture, instance.bbCorner010.x, instance.bbCorner010.y, dotSize, dotSize);
//                        hudBatch.draw(dotTexture, instance.bbCorner001.x, instance.bbCorner001.y, dotSize, dotSize);
//                        hudBatch.draw(dotTexture, instance.bbCorner110.x, instance.bbCorner110.y, dotSize, dotSize);
//                        hudBatch.draw(dotTexture, instance.bbCorner101.x, instance.bbCorner101.y, dotSize, dotSize);
//                        hudBatch.draw(dotTexture, instance.bbCorner011.x, instance.bbCorner011.y, dotSize, dotSize);
//                        hudBatch.draw(dotTexture, instance.bbCorner111.x, instance.bbCorner111.y, dotSize, dotSize);
                    }
                }
            }
            camera.project(screenCenter);
        }
        if (firing) {
            currentFrame = shootAnimation.getKeyFrame(stateTime, true);
            hudBatch.draw(currentFrame, Gdx.graphics.getWidth() / 2 - currentFrame.getRegionWidth() / 2, 0, currentFrame.getRegionWidth(), Gdx.graphics.getHeight() / 2);
        }
        hudBatch.draw(currentCRTexture, screenCenter.x - crosshairSize / 2, screenCenter.y - crosshairSize / 2, crosshairSize, crosshairSize);
        hudBatch.end();
        currentCRTexture = crossHairs.get(0);
    }

    private synchronized void calculateHudArrowPositions() {
        for (EnemyInstance instance : EnemyInstance.list) {
            indicatorPos.setZero();
            instance.transform.getTranslation(indicatorPos);
            instancePos.setZero();
            instance.transform.getTranslation(instancePos);
            boolean isBack = pointIsBack(indicatorPos);
            if (!camera.frustum.pointInFrustum(instancePos)) {
                camera.project(indicatorPos);
                camera.project(instancePos);
                float width = hudCam.viewportWidth;
                float height = hudCam.viewportHeight;
                if (indicatorPos.x < 0) {
                    indicatorPos.x = 0;
                }
                if (indicatorPos.x > width) {
                    indicatorPos.x = width;
                }
                if (indicatorPos.y < 0) {
                    indicatorPos.y = 0;
                }
                if (indicatorPos.y > height) {
                    indicatorPos.y = height;
                }
                if (isBack) {
                    if (indicatorPos.y < height / 2) {
                        indicatorPos.y = height;
                    } else {
                        indicatorPos.y = 0;
                    }
                    if (indicatorPos.x == 0) {
                        indicatorPos.x = width;
                    } else if (indicatorPos.x == width) {
                        indicatorPos.x = 0;
                    } else {
                        indicatorPos.x = width - indicatorPos.x;
                    }
                }
                instance.arrowPosition[0] = indicatorPos.x;
                instance.arrowPosition[1] = indicatorPos.y;
                instance.arrowPosition[2] = indicatorPos.z;
                instance.arrowPosition[3] = (float) calcRotationAngleInDegrees(width / 2, height / 2, instancePos);
                instance.notInFrustum = true;
            } else {
                instance.notInFrustum = false;
            }
        }
    }

    /**
     * Returns whether the point is in the frustum.
     * @param point The point
     * @return Whether the point is in the frustum.
     */
    public boolean pointIsBack(Vector3 point) {
        return camera.frustum.planes[0].isFrontFacing(point);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            firing = true;
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            firing = false;
            return true;
        }
        return false;
    }

    public PerspectiveCamera getCamera() {
        return camera;
    }
}
