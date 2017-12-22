package me.kalmanbncz.nd.ar;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader;
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem;
import me.kalmanbncz.nd.util.Log;

/**
 * The asset manager (global)
 * Created by Kalman on 2/6/2015.
 */
public class Assets {
    public static AssetManager manager;

    public static TextureAtlas myGraphics;

    public static TextureAtlas myOtherGraphics;

    public static boolean loading;

    public static void create() {
        manager = new AssetManager();
        load();
    }

    private static void load() {
        manager = new AssetManager();
        manager.load("models/brain-r.g3db", Model.class);
        TextureLoader.TextureParameter skyTextureParam = new TextureLoader.TextureParameter();
        skyTextureParam.genMipMaps = false;
        skyTextureParam.magFilter = Texture.TextureFilter.Linear;
        skyTextureParam.minFilter = Texture.TextureFilter.Linear;

        manager.load("particle/pre_particle.png", Texture.class, skyTextureParam);
        loading = true;
    }

    public static void done() {
    }

    public static void loadParticleEffects(ParticleSystem particleSystem) {
        ParticleEffectLoader.ParticleEffectLoadParameter loadParam = new ParticleEffectLoader.ParticleEffectLoadParameter(particleSystem.getBatches());
        ParticleEffectLoader loader = new ParticleEffectLoader(new InternalFileHandleResolver());
        manager.setLoader(ParticleEffect.class, loader);
        manager.load("particle/bullet-hit.pfx", ParticleEffect.class, loadParam);
        manager.load("particle/blue-explosion.pfx", ParticleEffect.class, loadParam);

        Log.log("reached effects");
//        manager.finishLoading();
    }

    public static void dispose() {
        manager.dispose();
    }
}
