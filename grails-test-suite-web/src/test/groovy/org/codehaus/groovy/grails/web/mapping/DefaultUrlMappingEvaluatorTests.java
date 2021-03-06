package org.codehaus.groovy.grails.web.mapping;

import grails.util.GrailsWebUtil;
import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

import java.util.List;

import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.codehaus.groovy.grails.validation.Constraint;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.mock.web.MockServletContext;

public class DefaultUrlMappingEvaluatorTests extends AbstractGrailsMappingTests {

    @SuppressWarnings("rawtypes")
    public void testNamedMappings() throws Exception {
        GroovyShell shell = new GroovyShell();
        Binding binding = new Binding();
        Script script = shell.parse("mappings = {\n" +
                "name firstMapping: \"/first/one\" {\n" +
                "}\n" +
                "\"/second/one\" {" +
                "}\n" +
                "name thirdMapping: \"/third/one\" {\n" +
                "}\n" +
        "}");

        script.setBinding(binding);
        script.run();

        Closure closure = (Closure)binding.getVariable("mappings");
        List mappings = evaluator.evaluateMappings(closure);
        assertEquals(3, mappings.size());
    }

    @SuppressWarnings("rawtypes")
    public void testNewMethod () throws Exception {
        GroovyShell shell = new GroovyShell ();
        Binding binding = new Binding();
        Script script = shell.parse (
                "mappings = {\n" +
                "    \"/$controller/$action?/$id?\" { \n" +
                "        constraints {\n" +
                "            id(matches:/\\d+/)\n" +
                "        }\n" +
                "    }\n" +
                "}\n");

        script.setBinding(binding);
        script.run();

        Closure closure = (Closure) binding.getVariable("mappings");
        List mappings = evaluator.evaluateMappings(closure);

        assertEquals(1, mappings.size());

        UrlMapping mapping = (UrlMapping) mappings.get(0);

        assertNull(mapping.getActionName());
        assertNull(mapping.getControllerName());
        assertEquals("(*)",mapping.getUrlData().getTokens()[0]);
        assertEquals("(*)?",mapping.getUrlData().getTokens()[1]);
        assertEquals("(*)?",mapping.getUrlData().getTokens()[2]);

        assertNotNull(mapping.getConstraints());

        assertTrue(makeSureMatchesConstraintExistsOnId(mapping));

        GrailsWebRequest r = GrailsWebUtil.bindMockWebRequest();

        UrlMappingInfo info = mapping.match("/mycontroller");
        info.configure(r);

        assertEquals("mycontroller", info.getControllerName());
        assertNull(mapping.match("/mycontroller").getActionName());
        assertNull(mapping.match("/mycontroller").getId());

        UrlMappingInfo info2 = mapping.match("/mycontroller/test");
        info2.configure(r);
        assertEquals("test", info2.getActionName());
        assertNull(mapping.match("/mycontroller/test").getId());
        assertEquals("234", mapping.match("/blog/test/234").getId());
    }

    @SuppressWarnings("rawtypes")
    public void testOldMethod () throws Exception {
        GroovyShell shell = new GroovyShell();
        Script script = shell.parse (
                "mappings {\n" +
                "    \"/$controller/$action?/$id?\" { \n" +
                "        constraints {\n" +
                "            id(matches:/\\d+/)\n" +
                "        }\n" +
                "    }\n" +
                "}\n");

        DefaultUrlMappingEvaluator evaluator = new DefaultUrlMappingEvaluator(new MockServletContext("/test"));
        List mappings = evaluator.evaluateMappings(script.getClass());
        assertEquals(1, mappings.size());
        assertNull(((UrlMapping) mappings.get(0)).getActionName());
        assertNull(((UrlMapping) mappings.get(0)).getControllerName());
        assertEquals("(*)",((UrlMapping) mappings.get(0)).getUrlData().getTokens()[0]);
        assertEquals("(*)?",((UrlMapping) mappings.get(0)).getUrlData().getTokens()[1]);
        assertEquals("(*)?",((UrlMapping) mappings.get(0)).getUrlData().getTokens()[2]);
    }

    private boolean makeSureMatchesConstraintExistsOnId(UrlMapping mapping) {
        ConstrainedProperty [] props = mapping.getConstraints();
        for (int i = 0; i < props.length; i++) {
            if ("id".equals(props[i].getPropertyName())) {
                Constraint [] constraints = props[i].getAppliedConstraints().toArray(new Constraint[0]);
                for (int j = 0; j < constraints.length; j++) {
                    if (constraints[j].getClass().getName().endsWith("MatchesConstraint")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
