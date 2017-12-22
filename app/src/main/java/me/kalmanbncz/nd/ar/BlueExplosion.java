package me.kalmanbncz.nd.ar;

import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.emitters.RegularEmitter;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class BlueExplosion {
    private static final Matrix4 mtx = new Matrix4();

    public static Array<BlueExplosion> list = new Array<>();

    public ParticleEffect pfx;

    public Vector3 location = Pooler.v3();

    // TODO pool this, create extendable PFX class for classes like BlueExplosion and BulletHit
    public BlueExplosion(Vector3 loc) {
        pfx = Particles.inst.obtainBlueExplosion();
        //mtx.setToScaling(0.5f, 0.5f, 0.5f);
        mtx.setToTranslation(loc);
        pfx.setTransform(mtx);
        Particles.inst.system.add(pfx);
        RegularEmitter emitter = (RegularEmitter) pfx.getControllers().first().emitter;
        emitter.setEmissionMode(RegularEmitter.EmissionMode.EnabledUntilCycleEnd);
        pfx.start();
        list.add(this);
    }

    public static void updateAll(float timeStep) {
        for (BlueExplosion blueExplosion : list) {
            blueExplosion.update();
        }
    }

    private void destroy() {
        Pooler.free(location);
        list.removeValue(this, true);
    }

    public void update() {
        RegularEmitter emitter = (RegularEmitter) pfx.getControllers().first().emitter;
        if (emitter.isComplete()) {
            destroy();
        }
    }
}
