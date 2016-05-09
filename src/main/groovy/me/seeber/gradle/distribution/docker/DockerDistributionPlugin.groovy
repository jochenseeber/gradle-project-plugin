/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2015, Jochen Seeber
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package me.seeber.gradle.distribution.docker

import groovy.transform.TypeChecked
import me.seeber.gradle.plugin.AbstractProjectPlugin
import me.seeber.gradle.project.base.BaseProjectPlugin

import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.War

import com.bmuschko.gradle.docker.DockerExtension
import com.bmuschko.gradle.docker.DockerRegistryCredentials
import com.bmuschko.gradle.docker.DockerRemoteApiPlugin
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage

@TypeChecked
class DockerDistributionPlugin extends AbstractProjectPlugin<DockerDistributionExtension> {

    DockerDistributionPlugin() {
        super("dockerConfig", DockerDistributionExtension)
    }

    void initialize() {
        project.plugins.apply(BaseProjectPlugin)
        project.plugins.apply(DockerRemoteApiPlugin)

        extension(DockerExtension).with {
            System.getenv()["DOCKER_HOST"]?.identity { String host ->
                url = host.replaceFirst("tcp://", "https://")
            }

            registryCredentials = new DockerRegistryCredentials()
            registryCredentials.with {
                url = "https://docker.caperwhite.com/v1/"
                username = getPropertyValue("docker.user")
                password = getPropertyValue("docker.password")
                email = getPropertyValue("docker.email")
            }
        }
    }

    protected void complete() {
        project.with {
            plugins.withType(WarPlugin) {
                War war = tasks.withType(War).getByName("war")
                Task warTask = tasks.getByName("war")

                Copy copyBaseFilesTask = tasks.create("dockerBaseCopyFiles", Copy)
                copyBaseFilesTask.with {
                    description = "Copy base image files to docker build directory"
                    group = "docker"

                    into "build/docker/base"

                    from("src/docker/base") { expand project: project }

                    from(configurations.getByName("runtime")) {
                        into "libs"
                        exclude config.volatileLibs
                    }

                    dependsOn warTask
                }


                DockerBuildImage buildBaseImageTask = tasks.create("dockerBaseBuildImage", DockerBuildImage)
                buildBaseImageTask.with {
                    description = "Build base image"
                    group = "docker"

                    tag = "${config.baseImage}:${version}"
                    inputDir = file("build/docker/base")

                    if (!project.hasProperty("bootstrap")) {
                        doFirst { checkDockerCredentials() }
                        pull = true
                    }

                    dependsOn copyBaseFilesTask
                }

                DockerPushImage pushBaseImageTask = tasks.create("dockerBasePushImage", DockerPushImage)
                pushBaseImageTask.with {
                    description = "Push base image to repository"
                    group = "docker"

                    imageName = config.baseImage
                    tag = version

                    doFirst { checkDockerCredentials() }
                    dependsOn buildBaseImageTask
                }

                Copy copyTomcatFilesTask = tasks.create("dockerTomcatCopyFiles", Copy)
                copyTomcatFilesTask.with {
                    description = "Copy tomcat image files to docker build directory"
                    group = "docker"

                    into "build/docker/tomcat"

                    from("src/docker/tomcat") { expand project: project }

                    from(configurations.getByName("runtime")) {
                        into "libs"
                        include config.volatileLibs
                    }

                    with copySpec {
                        with war
                        into "webapp"
                        exclude "*.jar"
                    }

                    dependsOn warTask
                }

                DockerBuildImage buildTomcatImageTask = tasks.create("dockerTomcatBuildImage", DockerBuildImage)
                buildTomcatImageTask.with {
                    description = "Build tomcat image"
                    group = "docker"

                    tag = "${config.image}:${version}"
                    inputDir = file("build/docker/tomcat")

                    if (!project.hasProperty("bootstrap")) {
                        doFirst { checkDockerCredentials() }
                        pull = true
                    }

                    // TODO dependsOn buildBaseImageTask
                    dependsOn copyTomcatFilesTask
                }

                DockerPushImage pushTomcatImageTask = tasks.create("dockerTomcatPushImage", DockerPushImage)
                pushTomcatImageTask.with {
                    description = "Push docker image to repository"
                    group = "docker"

                    imageName = config.image
                    tag = version

                    doFirst { checkDockerCredentials() }
                    buildTomcatImageTask
                    dependsOn buildTomcatImageTask
                }
            }
        }
    }

    protected void checkDockerCredentials() {
        extension(DockerExtension).registryCredentials.with {
            if(username == null || password == null || email == null) {
                throw new GradleException("You must set docker.user, docker.password and docker.email in '~/.gradle/gradle.properties'")
            }
        }
    }
}
