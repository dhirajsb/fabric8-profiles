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
package io.fabric8.profiles.forge.resource;

import java.io.File;
import java.io.IOException;

import org.jboss.forge.addon.resource.DirectoryResource;

/**
 * A {@link DirectoryResource} that represents a Fabric8 Profile.
 */
public interface ProfileResource extends DelegatingResource<DirectoryResource, File> {

    String getProfileName();

    Iterable<ProfileResource> getParentProfiles() throws IOException;

    void setParentProfiles(Iterable<ProfileResource> parents) throws IOException;

    void rename(String name);

    void copyTo(String name);
}
