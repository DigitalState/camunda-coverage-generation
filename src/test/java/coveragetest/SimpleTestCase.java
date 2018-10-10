package coveragetest;

import io.digitalstate.camunda.coverage.bpmn.CoverageBuilderJavaBridge;
import io.digitalstate.camunda.coverage.bpmn.bpmnjs.CssGeneration;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;

import org.junit.Rule;
import org.junit.Test;

/**
 * @author Daniel Meyer
 * @author Martin Schimak
 */
public class SimpleTestCase {

    @Rule
    public ProcessEngineRule rule = new ProcessEngineRule("camunda_config/camunda.cfg.xml");
    CoverageBuilderJavaBridge coverageBuilder = new CoverageBuilderJavaBridge();

    @Test
    @Deployment(resources = {"testProcess.bpmn"})
    public void shouldExecuteProcess() {
        // Set a Custom CSS File:
        // CssGeneration myCustomCSS = new CssGeneration();
        // myCustomCSS.setCssFile("/bpmn1.css");
        // coverageBuilder.setCssGeneration(myCustomCSS);

        // Given we create a new process instance
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("testProcess");
        // Then it should be active
        assertThat(processInstance).isActive();
        // And it should be the only instance
        assertThat(processInstanceQuery().count()).isEqualTo(1);
        // And there should exist just a single task within that process instance
        assertThat(task(processInstance)).isNotNull();

        // When we complete that task
        complete(task(processInstance));
        // Then the process instance should be ended
        assertThat(processInstance).isEnded();
        coverageBuilder.coverageSnapshot(processInstance);
        coverageBuilder.saveCoverageSnapshots();

    }

}