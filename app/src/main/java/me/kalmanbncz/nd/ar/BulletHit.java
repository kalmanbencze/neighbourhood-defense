package me.kalmanbncz.nd.ar;

import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.emitters.RegularEmitter;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import me.kalmanbncz.nd.ar.entity.EnemyInstance;


public class BulletHit {
    public static Array<BulletHit> list = new Array<>();

    private static Matrix4 mtx = new Matrix4();

    private static Vector3 tmp = Pooler.v3();

    private Vector3 location;

    private ParticleEffect pfx;

    private float range = 0f;

    public BulletHit(Vector3 loc, float range) {
        location = Pooler.v3().set(loc);
        this.range = range;
        pfx = Particles.inst.obtainBulletHit();
        //mtx.setToScaling(0.5f, 0.5f, 0.5f);
        mtx.setToTranslation(loc);
        pfx.setTransform(mtx);
        Particles.inst.system.add(pfx);
        RegularEmitter emitter = (RegularEmitter) pfx.getControllers().first().emitter;
        emitter.setEmissionMode(RegularEmitter.EmissionMode.EnabledUntilCycleEnd);
        pfx.start();

        list.add(this);

        applyEffects();
    }

    public static void updateAll(float timeStep) {
        for (BulletHit bulletHit : list) {
            bulletHit.update();
        }
        //Log.log("bhit list: " + list.size);
    }

    private void applyEffects() {
        float minDist = 5.0f;
        for (EnemyInstance ent : EnemyInstance.list) {
            Vector3 position = ent.getPosition();
            float distSqr = position.dst(location);
            if (distSqr <= minDist) {
                float strength = 10.0f;
                tmp.set(ent.getPosition()).sub(location).nor().scl(strength);
                tmp.y = 0f;
                ent.adjustVelocity(tmp);
                // TODO don't damage players yet, no respawn or death code exists
                if (!ent.isPlayer) {
                    ent.applyDamage(10f);
                }
            }
        }
    }

    private void destroy() {
        Pooler.free(location);
        Particles.inst.freeBulletHit(pfx);

        list.removeValue(this, true);
    }

    public void update() {
        RegularEmitter emitter = (RegularEmitter) pfx.getControllers().first().emitter;
//        emitter.update();
        if (emitter.isComplete()) {
            destroy();
        }
    }
}
