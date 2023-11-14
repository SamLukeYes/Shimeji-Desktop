package com.group_finity.mascot.generic;

import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.environment.Environment;

import java.awt.*;

/**
 * Java Environmental information is difficult to get used to get in the JNI.
 * <p>
 * Original Author: Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * <p>
 * Currently developed by Shimeji-ee Group.
 */
class GenericEnvironment extends Environment {

    private Area activeIE = new Area();

    @Override
    public void tick() {
        super.tick();
        activeIE.setVisible(false);
    }

    @Override
    public void moveActiveIE(final Point point) {
    }

    @Override
    public void restoreIE() {

    }

    @Override
    public Area getWorkArea() {
        return getScreen();
    }

    @Override
    public Area getActiveIE() {
        return activeIE;
    }

    @Override
    public String getActiveIETitle() {
        return null;
    }

    @Override
    public void refreshCache() {
        // I feel so refreshed
    }
}
