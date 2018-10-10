package coveragetest

import io.digitalstate.camunda.coverage.bpmn.CoverageBuilder
import io.digitalstate.camunda.coverage.bpmn.SequenceFlowHistory
import io.digitalstate.camunda.coverage.bpmn.bpmnjs.CssGeneration
import org.junit.ClassRule
import spock.lang.Shared
import spock.lang.Specification

import org.camunda.bpm.engine.test.ProcessEngineRule
import org.camunda.bpm.engine.history.HistoricActivityInstance
import org.camunda.bpm.engine.runtime.ProcessInstance


//brings in Camunda BPM Assertion + AssertJ core.api.Assertions
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*
// http://joel-costigliola.github.io/assertj/core/api/index.html
// http://camunda.github.io/camunda-bpm-assert/apidocs/org/camunda/bpm/engine/test/assertions/ProcessEngineTests.html
// http://joel-costigliola.github.io/assertj/


import static org.camunda.spin.Spin.S

class CallActivitySingleFeatureSpec extends Specification implements CoverageBuilder, SequenceFlowHistory {

    @ClassRule
    @Shared ProcessEngineRule processEngineRule = new ProcessEngineRule('camunda_config/camunda.cfg.xml')
    @Shared String deploymentId

    def setupSpec(){

//        CssGeneration myCss = new CssGeneration()
//        myCss.setCssFile('/bpmn1.css')
//        setCssGeneration(myCss)

        def deployment = repositoryService().createDeployment()
                .addInputStream(getSequenceFlowFileName(), getSequenceFlowListenerScript())
                .addModelInstance('CallActivityCoverage.bpmn', addSequenceFlowListeners('bpmn/CallActivityCoverage.bpmn'))
                .addModelInstance('CallActivityCoverage2.bpmn', addSequenceFlowListeners('bpmn/CallActivityCoverage2.bpmn'))
                .name('CallActivitiesCoverage')
                .enableDuplicateFiltering(false)
                .deploy()
        deploymentId = deployment.getId()
        println "Deployment ID: '${deploymentId}' has been created"
    }

    def 'Manage CallActivityCoverage1'() {
        when: 'Setting up variables'
        def json = S("{\"customer\": \"Kermit\"}")
        def startingVariables = [
                'json': json
        ]

        and: 'We start the CallActivityCoverage process definition'
        ProcessInstance callActivityCoverage1ProcessInstance = runtimeService().startProcessInstanceByKey('CallActivityCoverage')

        then: 'Process is Active and waiting for user task completion'
        assertThat(callActivityCoverage1ProcessInstance).isActive()

        then: 'The current process variables are equal to the starting variables'
        def processVariables = runtimeService().getVariables(callActivityCoverage1ProcessInstance.getProcessInstanceId())
        assertThat(processVariables == startingVariables)

        then: 'The process instance should be waiting for the Call Activity to Complete'
        assertThat(callActivityCoverage1ProcessInstance).isWaitingAt('Task_1gdn63n')

        coverageSnapshot(callActivityCoverage1ProcessInstance, 'some1')

        and: 'get the called called process instance'
        HistoricActivityInstance callActInstance = historyService().createHistoricActivityInstanceQuery()
                .processInstanceId(callActivityCoverage1ProcessInstance.getProcessInstanceId())
                .activityId('Task_1gdn63n')
                .singleResult()

        ProcessInstance callActivityCoverage2ProcessInstance = calledProcessInstance(processInstanceQuery().processInstanceId(callActInstance.getCalledProcessInstanceId()))

        then: 'CallActivityCoverage2 is running'
        assertThat(callActivityCoverage2ProcessInstance).isActive()

        then: 'CallActivityCoverage2 is waiting at the User Task'
        assertThat(callActivityCoverage2ProcessInstance).isWaitingAt('Task_0xjkfyv')

        then: 'Complete the User Task'
        complete(task(callActivityCoverage2ProcessInstance))

        then: 'CallActivityCoverage2 has completed'
        assertThat(callActivityCoverage2ProcessInstance).isEnded()

        coverageSnapshot(callActivityCoverage2ProcessInstance, 'some2')

        then: 'CallActivityCoverage1 has ended'
        assertThat(callActivityCoverage1ProcessInstance).isEnded()

        coverageSnapshot(callActivityCoverage1ProcessInstance)
    }

    def cleanupSpec() {
        saveCoverageSnapshots()
        saveCoverageSnapshots()

        repositoryService().deleteDeployment(deploymentId,
                true, // cascade
                true, // skipCustomListeners
                true) // skipIoMappings
        println "Deployment ID: '${deploymentId}' has been deleted"
    }

}

