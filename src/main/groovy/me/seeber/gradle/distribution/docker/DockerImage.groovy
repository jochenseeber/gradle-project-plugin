package me.seeber.gradle.distribution.docker

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySpec

class DockerImage implements Named {

    final Project project

    final String name

    final CopySpec copySpec = project.copySpec()

    final Set<Task> dependsOn = []

    String repository

    String tag

    DockerImage(Project project, String name) {
        this.project = project
        this.name = name
        this.tag = project.version
    }

    void from(Object from, Closure configuration) {
        copySpec.from(from, configuration);
    }

    void with(CopySpec... copySpecs) {
        copySpec.with(copySpecs)
    }

    void dependsOn(Task... tasks) {
        this.dependsOn.addAll(tasks);
    }
}