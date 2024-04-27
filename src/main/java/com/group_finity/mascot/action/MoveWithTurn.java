package com.group_finity.mascot.action;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

import java.awt.*;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kilkakon
 * @since 1.0.8
 */
public class MoveWithTurn extends BorderedAction {

    private static final Logger log = Logger.getLogger(MoveWithTurn.class.getName());

    public static final String PARAMETER_TARGETX = "TargetX";

    private static final int DEFAULT_TARGETX = Integer.MAX_VALUE;

    public static final String PARAMETER_TARGETY = "TargetY";

    private static final int DEFAULT_TARGETY = Integer.MAX_VALUE;

    private boolean turning = false;

    public MoveWithTurn(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
        if (animations.size() < 2) {
            throw new IllegalArgumentException("animations.size<2");
        }
    }

    @Override
    public boolean hasNext() throws VariableException {

        final int targetX = getTargetX();
        final int targetY = getTargetY();

        boolean noMoveX = false;
        boolean noMoveY = false;

        if (targetX != Integer.MIN_VALUE) {
            // Do we need to move in the X direction?
            if (getMascot().getAnchor().x == targetX) {
                noMoveX = true;
            }
        }

        if (targetY != Integer.MIN_VALUE) {
            // Do we need to move in the Y direction?
            if (getMascot().getAnchor().y == targetY) {
                noMoveY = true;
            }
        }

        return super.hasNext() && !noMoveX && !noMoveY;
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {

        super.tick();

        if (getBorder() != null && !getBorder().isOn(getMascot().getAnchor())) {
            // The mascot is off the wall
            log.log(Level.INFO, "Lost ground ({0}, {1})", new Object[]{getMascot(), this});
            throw new LostGroundException();
        }

        int targetX = getTargetX();
        int targetY = getTargetY();

        boolean down = false;

        if (targetX != DEFAULT_TARGETX) {
            if (getMascot().getAnchor().x != targetX) {
                // activate turn animation if we change directions
                turning = turning || getMascot().getAnchor().x < targetX != getMascot().isLookRight();
                getMascot().setLookRight(getMascot().getAnchor().x < targetX);
            }
        }
        if (targetY != DEFAULT_TARGETY) {
            down = getMascot().getAnchor().y < targetY;
        }

        // check if turning animation has finished
        if (turning && getTime() >= getAnimation().getDuration()) {
            turning = false;
        }

        // Animate
        getAnimation().next(getMascot(), getTime());

        if (targetX != DEFAULT_TARGETX) {
            // If we went past the target, set ourselves to the target position
            if (getMascot().isLookRight() && getMascot().getAnchor().x >= targetX
                    || !getMascot().isLookRight() && getMascot().getAnchor().x <= targetX) {
                getMascot().setAnchor(new Point(targetX, getMascot().getAnchor().y));
            }
        }
        if (targetY != DEFAULT_TARGETY) {
            // If we went past the target, set ourselves to the target position
            if (down && getMascot().getAnchor().y >= targetY ||
                    !down && getMascot().getAnchor().y <= targetY) {
                getMascot().setAnchor(new Point(getMascot().getAnchor().x, targetY));
            }
        }

    }

    @Override
    protected Animation getAnimation() throws VariableException {
        // force to last animation if turning
        if (turning) {
            return getAnimations().get(getAnimations().size() - 1);
        } else {
            // had to expose both animations and variables for this
            // is there a better way?
            List<Animation> animations = getAnimations();
            for (int index = 0; index < animations.size() - 1; index++) {
                if (animations.get(index).isEffective(getVariables())) {
                    return animations.get(index);
                }
            }
        }

        return null;
    }

    private int getTargetX() throws VariableException {
        return eval(getSchema().getString(PARAMETER_TARGETX), Number.class, DEFAULT_TARGETX).intValue();
    }

    private int getTargetY() throws VariableException {
        return eval(getSchema().getString(PARAMETER_TARGETY), Number.class, DEFAULT_TARGETY).intValue();
    }
}
