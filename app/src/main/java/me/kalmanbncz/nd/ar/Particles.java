package me.kalmanbncz.nd.ar;

import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem;
import com.badlogic.gdx.graphics.g3d.particles.batches.PointSpriteParticleBatch;
import com.badlogic.gdx.utils.Pool;

public class Particles {
    public static Particles inst;

    public ParticleSystem system;

    public boolean effectsCreated;

    private PFXPool bulletHitPool;

    private PFXPool blueExplosionPool;

    public Particles() {
        inst = this;
        system = ParticleSystem.get();
        PointSpriteParticleBatch psBatch = new PointSpriteParticleBatch();
        psBatch.setCamera(Renderer.getInstance().getCamera());
        system.add(psBatch);
        Assets.loadParticleEffects(system);
    }

    public void createEffects() {
        if (!effectsCreated) {
            ParticleEffect bulletHit = Assets.manager.get("particle/bullet-hit.pfx");
            ParticleEffect blueExplosion = Assets.manager.get("particle/blue-explosion.pfx");
            bulletHitPool = new PFXPool(bulletHit);
            blueExplosionPool = new PFXPool(blueExplosion);
            effectsCreated = true;
        }
    }

    public ParticleEffect obtainBulletHit() {
        //Log.log("free bullet-hit count: " + bulletHitPool.getFree());
        return bulletHitPool.obtain();
    }

    public void freeBulletHit(ParticleEffect pfx) {
        system.remove(pfx);
        bulletHitPool.free(pfx);
    }

    public ParticleEffect obtainBlueExplosion() {
        //Log.log("free bullet-hit count: " + blueExplosionPool.getFree());
        return blueExplosionPool.obtain();
    }

    public void freeBlueExplosion(ParticleEffect pfx) {
        system.remove(pfx);
        blueExplosionPool.free(pfx);
    }


    // TODO initial capacity in constructor
    private static class PFXPool extends Pool<ParticleEffect> {
        private ParticleEffect sourceEffect;

        public PFXPool(ParticleEffect sourceEffect) {
            this.sourceEffect = sourceEffect;
        }

        @Override
        public void free(ParticleEffect pfx) {
            pfx.reset();
            super.free(pfx);
        }

        @Override
        protected ParticleEffect newObject() {
            ParticleEffect pfx = sourceEffect.copy();
            pfx.init();
            return pfx;
        }
    }
}
