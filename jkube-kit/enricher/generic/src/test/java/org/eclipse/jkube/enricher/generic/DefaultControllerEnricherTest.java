/**
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 *
 *     https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.jkube.enricher.generic;

import com.jayway.jsonpath.matchers.JsonPathMatchers;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import mockit.Expectations;
import mockit.Mocked;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Collections;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author kamesh
 */
public class DefaultControllerEnricherTest {

    @Mocked
    private JKubeEnricherContext context;

    @Mocked
    ImageConfiguration imageConfiguration;

    @Mocked
    JavaProject project;

    @Test
    public void checkReplicaCount() throws Exception {
        enrichAndAssert(1, 3);
    }

    @Test
    public void checkDefaultReplicaCount() throws Exception {
        enrichAndAssert(1, 1);
    }

    protected void enrichAndAssert(int sizeOfObjects, int replicaCount) throws com.fasterxml.jackson.core.JsonProcessingException {
        // Setup a sample docker build configuration
        final BuildConfiguration buildConfig = BuildConfiguration.builder()
            .port("8080")
            .build();

        final TreeMap controllerConfig = new TreeMap();
        controllerConfig.put("replicaCount", String.valueOf(replicaCount));

        setupExpectations(buildConfig, controllerConfig);
        // Enrich
        DefaultControllerEnricher controllerEnricher = new DefaultControllerEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder();
        controllerEnricher.create(PlatformMode.kubernetes, builder);

        // Validate that the generated resource contains
        KubernetesList list = builder.build();
        assertEquals(sizeOfObjects, list.getItems().size());

        String json = ResourceUtil.toJson(list.getItems().get(0));
        assertThat(json, JsonPathMatchers.isJson());
        assertThat(json, JsonPathMatchers.hasJsonPath("$.spec.replicas", Matchers.equalTo(replicaCount)));
    }

    protected void setupExpectations(final BuildConfiguration buildConfig, final TreeMap controllerConfig) {

        new Expectations() {{

            context.getGav();
            result = new GroupArtifactVersion("", "jkube-controller-test", "0");

            Configuration config =
                Configuration.builder()
                    .processorConfig(new ProcessorConfig(null, null,
                                                         Collections.singletonMap("jkube-controller", controllerConfig)))
                    .image(imageConfiguration)
                    .build();
            context.getConfiguration();
            result = config;

            imageConfiguration.getBuildConfiguration();
            result = buildConfig;

            imageConfiguration.getName();
            result = "helloworld";

        }};
    }
}
