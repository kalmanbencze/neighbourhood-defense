package me.kalmanbncz.nd.ar;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

/**
 * count objects obtained and freed to aid in checking whether all obtained objects are eventually freed
 */
public abstract class CountingPool<T> extends Pool<T> {
    int obtained;

    int freed;

    @Override
    public T obtain() {
        obtained++;
        return super.obtain();
    }

    @Override
    public void free(T object) {
        freed++;
        super.free(object);
    }

    @Override
    public void freeAll(Array<T> objects) {
        freed += objects.size;
        super.freeAll(objects);
    }
}
