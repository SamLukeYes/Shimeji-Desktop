package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.behavior.UserBehavior;
import com.group_finity.mascot.exception.ActionInstantiationException;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import com.joconner.i18n.Utf8ResourceBundleControl;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Original Author: Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * <p>
 * Currently developed by Shimeji-ee Group.
 */
public class Configuration {
    private static final Logger log = Logger.getLogger(Configuration.class.getName());
    private final Map<String, ActionBuilder> actionBuilders = new LinkedHashMap<>();
    private final Map<String, BehaviorBuilder> behaviorBuilders = new LinkedHashMap<>();
    private ResourceBundle schema;

    public void load(final Entry configurationNode, final String imageSet) throws IOException, ConfigurationException {
        log.log(Level.INFO, "Start Reading Configuration File...");

        // prepare schema
        ResourceBundle.Control utf8Control = new Utf8ResourceBundleControl(false);
        Locale locale;

        // check for Japanese XML tag and adapt locale accordingly
        if (configurationNode.hasChild("\u52D5\u4F5C\u30EA\u30B9\u30C8") ||
                configurationNode.hasChild("\u884C\u52D5\u30EA\u30B9\u30C8")) {
            log.log(Level.INFO, "Using ja-JP schema");
            locale = Locale.forLanguageTag("ja-JP");
        } else {
            log.log(Level.INFO, "Using en-US schema");
            locale = Locale.forLanguageTag("en-US");
        }

        File file = new File("./conf");
        URL[] urls = {file.toURI().toURL()};
        try (URLClassLoader loader = new URLClassLoader(urls)) {
            schema = ResourceBundle.getBundle("schema", locale, loader, utf8Control);
        }

        for (final Entry list : configurationNode.selectChildren(schema.getString("ActionList"))) {
            log.log(Level.INFO, "Action List...");

            for (final Entry node : list.selectChildren(schema.getString("Action"))) {
                final ActionBuilder action = new ActionBuilder(this, node, imageSet);

                if (getActionBuilders().containsKey(action.getName())) {
                    throw new ConfigurationException(Main.getInstance().getLanguageBundle().getString("DuplicateActionErrorMessage") + ": " + action.getName());
                }

                getActionBuilders().put(action.getName(), action);
            }
        }

        for (final Entry list : configurationNode.selectChildren(schema.getString("BehaviourList"))) {
            log.log(Level.INFO, "Behavior List...");

            loadBehaviors(list, new ArrayList<>());
        }

        log.log(Level.INFO, "Configuration loaded successfully");
    }

    private void loadBehaviors(final Entry list, final List<String> conditions) {
        for (final Entry node : list.getChildren()) {
            if (node.getName().equals(schema.getString("Condition"))) {
                final List<String> newConditions = new ArrayList<>(conditions);
                newConditions.add(node.getAttribute(schema.getString("Condition")));

                loadBehaviors(node, newConditions);
            } else if (node.getName().equals(schema.getString("Behaviour"))) {
                final BehaviorBuilder behavior = new BehaviorBuilder(this, node, conditions);
                getBehaviorBuilders().put(behavior.getName(), behavior);
            }
        }
    }

    public Action buildAction(final String name, final Map<String, String> params) throws ActionInstantiationException {

        final ActionBuilder factory = actionBuilders.get(name);
        if (factory == null) {
            throw new ActionInstantiationException(Main.getInstance().getLanguageBundle().getString("NoCorrespondingActionFoundErrorMessage") + ": " + name);
        }

        return factory.buildAction(params);
    }

    public void validate() throws ConfigurationException {

        for (final ActionBuilder builder : getActionBuilders().values()) {
            builder.validate();
        }
        for (final BehaviorBuilder builder : getBehaviorBuilders().values()) {
            builder.validate();
        }
    }

    public Behavior buildBehavior(final String previousName, final Mascot mascot) throws BehaviorInstantiationException {

        final VariableMap context = new VariableMap();
        context.put("mascot", mascot);

        final List<BehaviorBuilder> candidates = new ArrayList<>();
        long totalFrequency = 0;
        for (final BehaviorBuilder behaviorFactory : getBehaviorBuilders().values()) {
            try {
                if (behaviorFactory.isEffective(context)) {
                    candidates.add(behaviorFactory);
                    totalFrequency += behaviorFactory.getFrequency();
                }
            } catch (final VariableException e) {
                log.log(Level.WARNING, "An error occurred calculating the frequency of the action", e);
            }
        }

        if (previousName != null) {
            final BehaviorBuilder previousBehaviorFactory = getBehaviorBuilders().get(previousName);
            if (!previousBehaviorFactory.isNextAdditive()) {
                totalFrequency = 0;
                candidates.clear();
            }
            for (final BehaviorBuilder behaviorFactory : previousBehaviorFactory.getNextBehaviorBuilders()) {
                try {
                    if (behaviorFactory.isEffective(context)) {
                        candidates.add(behaviorFactory);
                        totalFrequency += behaviorFactory.getFrequency();
                    }
                } catch (final VariableException e) {
                    log.log(Level.WARNING, "An error occurred calculating the frequency of the behavior", e);
                }
            }
        }

        if (totalFrequency == 0) {
            if (Boolean.parseBoolean(Main.getInstance().getProperties().getProperty("Multiscreen", "true"))) {
                mascot.setAnchor(new Point((int) (Math.random() * (mascot.getEnvironment().getScreen().getRight() - mascot.getEnvironment().getScreen().getLeft())) + mascot.getEnvironment().getScreen().getLeft(),
                        mascot.getEnvironment().getScreen().getTop() - 256));
            } else {
                mascot.setAnchor(new Point((int) (Math.random() * (mascot.getEnvironment().getWorkArea().getRight() - mascot.getEnvironment().getWorkArea().getLeft())) + mascot.getEnvironment().getWorkArea().getLeft(),
                        mascot.getEnvironment().getWorkArea().getTop() - 256));
            }
            return buildBehavior(schema.getString(UserBehavior.BEHAVIOURNAME_FALL));
        }

        double random = Math.random() * totalFrequency;

        for (final BehaviorBuilder behaviorFactory : candidates) {
            random -= behaviorFactory.getFrequency();
            if (random < 0) {
                return behaviorFactory.buildBehavior();
            }
        }

        return null;
    }

    public Behavior buildBehavior(final String name) throws BehaviorInstantiationException {
        if (behaviorBuilders.containsKey(name)) {
            return getBehaviorBuilders().get(name).buildBehavior();
        } else {
            throw new BehaviorInstantiationException(Main.getInstance().getLanguageBundle().getString("NoBehaviourFoundErrorMessage") + " (" + name + ")");
        }
    }

    Map<String, ActionBuilder> getActionBuilders() {
        return actionBuilders;
    }

    private Map<String, BehaviorBuilder> getBehaviorBuilders() {
        return behaviorBuilders;
    }

    public Set<String> getBehaviorNames() {
        return behaviorBuilders.keySet();
    }

    public ResourceBundle getSchema() {
        return schema;
    }
}
