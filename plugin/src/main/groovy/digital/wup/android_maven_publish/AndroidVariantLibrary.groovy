/*
 * Copyright 2017 W.UP Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package digital.wup.android_maven_publish

import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.attributes.Usage
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin

final class AndroidVariantLibrary implements SoftwareComponentInternal {

    private final Set<UsageContext> _usages
    private final PublishConfiguration publishConfiguration

    AndroidVariantLibrary(ObjectFactory objectFactory, ConfigurationContainer configurations, PublishConfiguration publishConfiguration) {
        this.publishConfiguration = publishConfiguration

        final UsageContext compileUsage = new CompileUsage(configurations, publishConfiguration, objectFactory.named(Usage.class, Usage.JAVA_API))
        final UsageContext runtimeUsage = new RuntimeUsage(configurations, publishConfiguration, objectFactory.named(Usage.class, Usage.JAVA_RUNTIME))

        def usages = [compileUsage]
        if (configurations.findByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)) {
            usages += runtimeUsage
        }
        _usages = Collections.unmodifiableSet(usages.toSet())
    }
    
    @Override
    Set<UsageContext> getUsages() {
        return _usages
    }

    @Override
    String getName() {
        return publishConfiguration.name
    }

    private static class CompileUsage extends BaseUsage {

        private DependencySet dependencies

        CompileUsage(ConfigurationContainer configurations, PublishConfiguration publishConfiguration, Usage usage) {
            super(configurations, publishConfiguration, usage)
        }

        @Override
        Set<ModuleDependency> getDependencies() {
            if (dependencies == null) {
                def apiElements = publishConfiguration.publishConfig + JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME.capitalize()
                if (configurations.findByName(apiElements)) {
                    dependencies = configurations.findByName(apiElements).allDependencies
                } else {
                    dependencies = configurations.findByName('default').allDependencies
                }
            }
            return dependencies.withType(ModuleDependency)
        }
    }

    private static class RuntimeUsage extends BaseUsage {

        private DependencySet dependencies

        RuntimeUsage(ConfigurationContainer configurations, PublishConfiguration publishConfiguration, Usage usage) {
            super(configurations, publishConfiguration, usage)
        }

        @Override
        Set<ModuleDependency> getDependencies() {
            if (dependencies == null) {
                def runtimeElements = publishConfiguration.publishConfig + JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME.capitalize()
                dependencies = configurations.findByName(runtimeElements).allDependencies
            }
            return dependencies.withType(ModuleDependency)
        }
    }

    private static abstract class BaseUsage implements UsageContext {
        protected final ConfigurationContainer configurations
        protected final PublishConfiguration publishConfiguration;
        protected final Usage usage;

        BaseUsage(ConfigurationContainer configurations, PublishConfiguration publishConfiguration, Usage usage) {
            this.configurations = configurations
            this.publishConfiguration = publishConfiguration
            this.usage = usage;
        }

        @Override
        Usage getUsage() {
            return usage;
        }

        @Override
        Set<PublishArtifact> getArtifacts() {
            return publishConfiguration.artifacts
        }
    }
}
