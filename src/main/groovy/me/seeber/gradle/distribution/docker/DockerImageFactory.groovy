package me.seeber.gradle.distribution.docker

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project

class DockerImageFactory implements NamedDomainObjectFactory<DockerImage> {

    final Project project;

    DockerImageFactory(Project project) {
        this.project = project
    }

    DockerImage create(String name) {
        return new DockerImage(project, name)
    }
}