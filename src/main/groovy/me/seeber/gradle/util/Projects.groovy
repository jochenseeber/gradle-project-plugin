package me.seeber.gradle.util

import groovy.transform.TypeChecked

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier

@TypeChecked
class Projects {

    static String lookupResolvedVersion(Project project, String group, String name) {
        String version = null

        project.configurations.find { Configuration configuration ->
            configuration.incoming.resolutionResult.allComponents*.moduleVersion.find { ModuleVersionIdentifier module ->
                if(module.group == group && module.name == name) {
                    version = module.version
                }
            }
        }

        version
    }
}
