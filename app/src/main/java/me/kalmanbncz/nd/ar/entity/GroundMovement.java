package me.kalmanbncz.nd.ar.entity;

import com.badlogic.gdx.math.Vector3;

public class GroundMovement extends Movement {

    public GroundMovement(int entityId) {
        super(entityId);
    }

    @Override
    void accelerate(float timeStep) {
        Vector3 diff = tmp;
        diff.set(destination).sub(position);
        // no vertical velocity, no matter the height of the destination
        diff.y = 0f;
        acceleration.set(diff).nor().scl(accelRate).scl(timeStep);
        //Log.log("ground accelerate: " + Tools.fmt(acceleration));
        velocity.add(acceleration);
    }
}
