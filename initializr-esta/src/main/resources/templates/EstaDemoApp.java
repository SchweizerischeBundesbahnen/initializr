${packageName};

import org.springframework.boot.SpringApplication;
<% demoAppImports.each { %>
${it}<% } %>

<% demoAppAnnotations.each { %>
${it}<% } %>
public class ${applicationName}${nameSuffix}App {

    public static void main(String[] args) {
        SpringApplication.run(${applicationName}${nameSuffix}App.class, args);
    }
}
