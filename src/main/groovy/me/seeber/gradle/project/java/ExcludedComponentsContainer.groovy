package me.seeber.gradle.project.java;

import groovy.transform.TypeChecked

import org.gradle.api.internal.artifacts.DefaultExcludeRuleContainer

@TypeChecked
public class ExcludedComponentsContainer extends DefaultExcludeRuleContainer{

    public void exclude(Map<String, String> properties) {
        add(properties);
    }
}
