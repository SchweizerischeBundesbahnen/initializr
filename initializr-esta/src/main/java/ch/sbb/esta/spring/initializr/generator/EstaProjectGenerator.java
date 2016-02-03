package ch.sbb.esta.spring.initializr.generator;

import io.spring.initializr.generator.ProjectGenerator;
import io.spring.initializr.generator.ProjectGeneratorInterface;
import io.spring.initializr.generator.ProjectRequest;
import io.spring.initializr.util.GroovyTemplate;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class EstaProjectGenerator extends ProjectGenerator implements ProjectGeneratorInterface {

    private static final Logger log = LoggerFactory.getLogger(EstaProjectGenerator.class);

    /**
     * Generate a project structure for the specified {@link ProjectRequest}. Returns
     * a directory containing the project.
     */
    public File generateProjectStructure(ProjectRequest request) {

        File projectStructure = null;
        // generates esta project structure template, if requested. Otherwise generates the default initializr project structure.
        if (isEstaTemplateToBeGenerated(request)) {
            projectStructure = generateEstaProjectStructure(request);
        } else {
            projectStructure = super.generateProjectStructure(request);
        }
        return projectStructure;
    }

    private void createPom(LinkedHashMap model, File dir) throws Exception {
        String pom = new String(doGenerateEstaMavenPom(model));
        ResourceGroovyMethods.write(new File(dir, "pom.xml"), pom);
        writeMavenWrapper(dir);
    }

    protected File generateEstaProjectStructure(ProjectRequest request) {
        try {
            LinkedHashMap model = (LinkedHashMap) initializeModel(request);

            File rootDir = File.createTempFile("tmp", "", new File(getTmpdir()));
            addTempFile(rootDir.getName(), rootDir);
            rootDir.delete();
            rootDir.mkdirs();

            File dir = initializerProjectDir(rootDir, request);

            createPom(model, dir);

            String applicationName = request.getApplicationName().replace("Application", "");
            // by now only java is supported
            String language = "java";
            String codeLocation = language;

            File src = new File(new File(dir, "src/main/" + codeLocation), request.getPackageName().replace(".", "/"));
            src.mkdirs();

            if (request.getPackaging().equals("war")) {
                // TODO: verify, what to build here
                String fileName = "ServletInitializer." + language;
                write(new File(src, fileName), fileName, model);
            }

            File test = new File(new File(dir, "src/test/" + codeLocation), request.getPackageName().replace(".", "/"));
            test.mkdirs();
            if (request.hasWebFacet()) {
                model.put("testAnnotations", "@WebAppConfiguration\n");
                model.put("testImports", "import org.springframework.test.context.web.WebAppConfiguration;\n");
            } else {
                model.put("testAnnotations", "");
                model.put("testImports", "");
            }

            File resources = new File(dir, "src/main/resources");
            resources.mkdirs();
            ResourceGroovyMethods.write(new File(resources, "application.properties"), "");

            generateRepository(request, dir, applicationName, codeLocation, language, model);
            generateEstaBasisModule(request, dir, applicationName, codeLocation, language, model);

            if (request.hasWebFacet()) {
                new File(dir, "src/main/resources/templates").mkdirs();
                new File(dir, "src/main/resources/static").mkdirs();
            }

            invokeListeners(request);
            return ((File) (rootDir));

        } catch (Exception e) {
            log.error("Error trying to generate este Project structure", e);
            throw new RuntimeException(e);
        }
    }

    protected File generateEstaBasisModule(ProjectRequest request, File dir, String applicationName, String codeLocation, final String language, Map model) {

        File src = new File(new File(dir, "src/main/" + codeLocation), request.getPackageName().replace(".", "/"));
        src.mkdirs();
        createApp(request,src,applicationName, "Backend", codeLocation, language, model);

        File resources = new File(dir, "src/main/resources");
        resources.mkdirs();
        createConfiguration(request, resources, model);

        return ((File) (dir));
    }

    protected File createApp(ProjectRequest request, File src, String applicationName, String applicationSufix, String codeLocation, final String language, Map model) {

        model.put("nameSuffix", "Backend");
        write(new File(src, applicationName + applicationSufix + "." + language), "EstaDemoApp." + language, model);
        return ((File) (src));
    }

    protected File createConfiguration(ProjectRequest request, File resources, Map model) {
        write(new File(resources, "application.properties"), "application.properties", model);
        write(new File(resources, "banner.txt"), "banner.yml", model);
        write(new File(resources, "bootstrap.yml"), "bootstrap.yml", model);

        if("esta-demo-backend".equals(request.getEstaTemplate())){
            write(new File(resources, "application.yml"), "applicationBackend.yml", model);
        }
        if("esta-demo-customer".equals(request.getEstaTemplate())){
            write(new File(resources, "application.yml"), "applicationCustomer.yml", model);
        }
        return ((File) (resources));
    }

    protected File generateRepository(ProjectRequest request, File dir, final String applicationName, String codeLocation, final String language, Map model) {
        File src = new File(new File(dir, "src/main/" + codeLocation), request.getPackageName().replace(".", "/").concat("/repository"));
        src.mkdirs();
        write(new File(src, applicationName + "CustomerRepository." + language), "DemoRepository." + language, model);

        return ((File) (src));
    }

    @Override
    protected Map initializeModel(ProjectRequest request) {
        // enrich request with styles needed by esta projects
        // TODO: the list of styles must be validated and completed
        if("esta-demo-backend".equals(request.getEstaTemplate())){
            request.getStyle().addAll(Arrays.asList("actuator", "cloud-eureka", "cloud-oauth2", "cloud-hystrix", "spring-cloud-starter-config"));
        }
        if("esta-demo-customer".equals(request.getEstaTemplate())){
            request.getStyle().addAll(Arrays.asList("data-jpa", "cloud-eureka", "spring-cloud-starter-config"));
        }

        Map model = super.initializeModel(request);

        if("esta-demo-backend".equals(request.getEstaTemplate())){
            initializeModelBackendTemplate(model);
        }
        if("esta-demo-customer".equals(request.getEstaTemplate())){
            initializeModelCustomerTemplate(model);
        }

        return model;
    }

    private void initializeModelBackendTemplate(Map model){

        List<String> demoAppImports = Arrays.asList("import org.springframework.cloud.client.SpringCloudApplication;",
                "import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;",
                "import org.springframework.cloud.client.discovery.EnableDiscoveryClient;",
                "import org.springframework.context.annotation.Configuration;",
                "import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;");
        model.put("demoAppImports", demoAppImports);

        List<String> demoAppAnnotations = Arrays.asList("@SpringBootApplication", "@EnableDiscoveryClient");
        model.put("demoAppAnnotations", demoAppAnnotations);

        model.put("moduleId", "esta-demo-backend");
        model.put("bannerDescription", "ESTA Demo Backend");
        model.put("demoDescription", "Generated based on the ESTA Cloud Backend Template");
    }

    private void initializeModelCustomerTemplate(Map model){

        List<String> demoAppImports = Arrays.asList("import org.springframework.boot.autoconfigure.SpringBootApplication;",
                "import org.springframework.cloud.client.discovery.EnableDiscoveryClient;");
        model.put("demoAppImports", demoAppImports);

        List<String> demoAppAnnotations = Arrays.asList("@SpringCloudApplication",
                "@EnableDiscoveryClient",
                "@EnableCircuitBreaker",
                "@Configuration",
                "@EnableResourceServer)");
        model.put("demoAppAnnotations", demoAppAnnotations);

        model.put("moduleId", "esta-demo-customer");
        model.put("bannerDescription", "ESTA Demo Customer Microservice");
        model.put("demoDescription", "Generated based on the ESTA Cloud Customer Template");

    }

    protected byte[] doGenerateEstaMavenPom(Map model) {
        try {
            return GroovyTemplate.template("esta-starter-pom.xml", model).getBytes();
        } catch (Exception e) {
           log.error("Error generating esta maven pom", e);
           throw new RuntimeException(e);
        }
    }

    private boolean isEstaTemplateToBeGenerated(ProjectRequest request){
        return request.getEstaTemplate() != null && !"none".equals(request.getEstaTemplate());
    }
}
