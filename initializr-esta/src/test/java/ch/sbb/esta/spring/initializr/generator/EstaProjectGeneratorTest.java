/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2016.
 */

package ch.sbb.esta.spring.initializr.generator;

import ch.sbb.esta.spring.initializr.generator.EstaProjectGenerator;
import io.spring.initializr.generator.ProjectGenerator;
import io.spring.initializr.generator.ProjectGeneratorTests;
import io.spring.initializr.generator.ProjectRequest;
import io.spring.initializr.metadata.InitializrMetadata;
import io.spring.initializr.metadata.InitializrMetadataProvider;
import io.spring.initializr.test.InitializrMetadataTestBuilder;
import io.spring.initializr.test.ProjectAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

public class EstaProjectGeneratorTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private EstaProjectGenerator projectGenerator = new EstaProjectGenerator();

    @Before
    public void setup(){
        try {
            InitializrMetadata metadata = InitializrMetadataTestBuilder.withDefaults()
                    .addDependencyGroup("test", "web", "security", "data-jpa", "aop", "batch", "integration").build();
            applyMetadata(metadata);
            projectGenerator.setTmpdir(folder.newFolder().getAbsolutePath());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGenerateRepository() throws Exception {

        ProjectRequest request = createProjectRequest("web");
        request.setBootVersion("1.2.0.RC1");
        request.setName("MyDemo");
        request.setPackageName("foo");
        generateRepository(request, "java", "java").sourceCodeAssert("src/main/java/foo/repository/MyDemoCustomerRepository.java").hasImports(PagingAndSortingRepository.class.getName()).contains("@RepositoryRestResource");

    }

    @Test
    public void testGenerateEstaProjectStructure() throws Exception {

        ProjectRequest request = createProjectRequest("web");
        request.setBootVersion("1.2.0.RC1");
        request.setName("MyDemo");
        request.setPackageName("foo");
        generateEstaProjectStructure(request).sourceCodeAssert("src/main/java/foo/repository/MyDemoCustomerRepository.java").hasImports(PagingAndSortingRepository.class.getName()).contains("@RepositoryRestResource");

    }

    private void applyMetadata(final InitializrMetadata metadata) throws Exception {

        InitializrMetadataProvider metadataProvider = new InitializrMetadataProvider() {
            @Override
            public InitializrMetadata get() {
                return metadata;
            }
        };

        Field m = ProjectGenerator.class.getDeclaredField("metadataProvider");
        m.setAccessible(true);
        m.set(projectGenerator, metadataProvider);
    }

    ProjectRequest createProjectRequest(String... styles) {
        ProjectRequest request = new ProjectRequest();
        request.initialize(projectGenerator.getMetadataProvider().get());
        request.getStyle().addAll(Arrays.asList(styles));
        return request;
    }

    public ProjectAssert generateEstaProjectStructure(ProjectRequest request) throws Exception {
        File rootDir = getRootDir(request);
//        projectGenerator.generateEstaProjectStructure(request, rootDir, getModel(request));
        projectGenerator.generateEstaProjectStructure(request);
        return new ProjectAssert(rootDir);
    }

    public ProjectAssert generateRepository(ProjectRequest request, String codeLocation, String language) throws Exception {
        File rootDir = getRootDir(request);
        projectGenerator.generateRepository(request, rootDir, request.getName(), codeLocation, language, getModel(request));
        return new ProjectAssert(rootDir);
    }

    public Map getModel(ProjectRequest request) throws Exception{
        Method m = ProjectGenerator.class.getDeclaredMethod("initializeModel", ProjectRequest.class);
        m.setAccessible(true);
        return (Map)m.invoke(projectGenerator, new Object[]{request});
    }

    public File getRootDir(ProjectRequest request) throws Exception{
        File rootDir = File.createTempFile("tmp", "", new File(projectGenerator.getTmpdir()));
        rootDir.delete();
        rootDir.mkdirs();

        return ((File) (rootDir));
    }

    public File getDir(ProjectRequest request, File rootDir) throws Exception{
        Method m = ProjectGenerator.class.getDeclaredMethod("initializerProjectDir", File.class, ProjectRequest.class);
        m.setAccessible(true);
        return (File) m.invoke(projectGenerator, new Object[]{rootDir, request});
    }
}
