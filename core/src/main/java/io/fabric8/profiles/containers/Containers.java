/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.profiles.containers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.fabric8.profiles.Profiles;
import io.fabric8.profiles.ProfilesHelpers;
import io.fabric8.profiles.config.ContainerConfigDTO;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static io.fabric8.profiles.config.ConfigHelper.fromValue;
import static io.fabric8.profiles.config.ConfigHelper.toValue;
import static io.fabric8.profiles.containers.Constants.CONTAINERS;
import static io.fabric8.profiles.containers.Constants.CONTAINER_FIELD;
import static io.fabric8.profiles.containers.Constants.DEFAULT_CONTAINER_TYPE;

/**
 * Utility class for reading container configurations and generating containers.
 */
public class Containers {

    private final Path repository;
    private final Profiles profiles;
    private Map<String, ProjectReifier> reifierMap;

    /**
     * @param repository    container configuration directory.
     * @param reifierMap    map from container types to reifiers.
     * @param profiles      profiles repository.
     */
    public Containers(Path repository, Map<String, ProjectReifier> reifierMap, Profiles profiles) {
        this.repository = repository;
        this.reifierMap = reifierMap;
        this.profiles = profiles;
    }

    /**
     * @param target        is the directory where reified container will be written to.
     * @param name name of the container to reify.
     * @throws IOException  on error.
     */
    public void reify(Path target, String name) throws IOException {
        // read container config
        ObjectNode containerConfig = getContainerConfig(name);
        final ContainerConfigDTO config = toValue(containerConfig, ContainerConfigDTO.class);
        config.setName(name);
        if (config.getContainerType() == null) {
            config.setContainerType(DEFAULT_CONTAINER_TYPE);
        }
        containerConfig.set(CONTAINER_FIELD, fromValue(config).get(CONTAINER_FIELD));

        // container profiles and types
        List<String> containerProfiles = Arrays.asList(
            config.getProfiles().split(" "));
        final String[] containerType = config.getContainerType().split(" ");

        // get reifier from type
        final ProjectReifier[] reifiers = getProjectReifiers(containerType);

        // temp dir for materialized profile
        final Path profilesDir = Files.createTempDirectory(target, "profiles-");

        try {
            // remove ensemble profiles fabric-ensemble-* from fabric8 v1
            containerProfiles = containerProfiles.stream()
                .filter(p -> !p.matches("fabric\\-ensemble\\-.*"))
                .collect(Collectors.toList());

            // materialize profile
            profiles.materialize(profilesDir, containerProfiles.toArray(new String[containerProfiles.size()]));

            // reify
            for (ProjectReifier reifier : reifiers) {
                reifier.reify(target, containerConfig, profilesDir);
            }

        } finally {
            try {
                ProfilesHelpers.deleteDirectory(profilesDir);
            } catch (IOException ignore) {
            }
        }
    }

    private ProjectReifier[] getProjectReifiers(String[] containerType) throws IOException {
        final List<ProjectReifier> reifiers = new ArrayList<ProjectReifier>();
        for (String type : containerType) {
            final ProjectReifier reifier = reifierMap.get(type);
            if (reifier == null) {
                throw new IOException("Unknown container type " + type);
            }
            reifiers.add(reifier);
        }
        return reifiers.toArray(new ProjectReifier[reifiers.size()]);
    }

    private ObjectNode getContainerConfig(String name) throws IOException {
        final Path configPath = repository.resolve(String.format(CONTAINERS, name));
        if (!Files.exists(configPath)) {
            throw new IOException("Missing container config " + configPath);
        }
        return (ObjectNode) ProfilesHelpers.readYamlFile(configPath);
    }
}
