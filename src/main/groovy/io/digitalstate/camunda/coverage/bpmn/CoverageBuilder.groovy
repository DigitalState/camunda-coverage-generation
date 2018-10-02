package io.digitalstate.camunda.coverage.bpmn

import org.apache.commons.io.FileUtils
import org.camunda.bpm.engine.history.HistoricDetail
import org.camunda.bpm.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity

import org.camunda.bpm.engine.runtime.ProcessInstance
import org.camunda.bpm.model.bpmn.Bpmn
import org.camunda.bpm.model.bpmn.BpmnModelInstance
import org.camunda.bpm.model.bpmn.instance.FlowNode
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent
import org.camunda.bpm.model.bpmn.instance.ReceiveTask
import org.camunda.bpm.model.bpmn.instance.ServiceTask
import org.camunda.bpm.model.bpmn.instance.UserTask
import org.codehaus.groovy.reflection.ReflectionUtils

import io.digitalstate.camunda.coverage.bpmn.bpmnjs.CssGeneration
import io.digitalstate.camunda.coverage.bpmn.bpmnjs.JsGeneration
import io.digitalstate.camunda.coverage.bpmn.bpmnjs.TemplateGeneration

import java.nio.file.Path
import java.nio.file.Paths

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.historyService
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.repositoryService

trait CoverageBuilder implements TemplateGeneration{

    static HashMap<String, CoverageData> coverageSnapshots = [:]

    void coverageSnapshot(ProcessInstance processInstance, String coverageDataName = UUID.randomUUID(), Integer coverageDataWeight = 0){
        CoverageData coverageData = generateCoverageData(processInstance, coverageDataName, coverageDataWeight)
        coverageSnapshots.put(coverageDataName, coverageData)
    }

    void saveCoverageSnapshots(HashMap<String, CoverageData> data = coverageSnapshots,
                               String buildDir = 'target') {

        FileTreeBuilder treeBuilder = new FileTreeBuilder()

        // @TODO review use cases of reflection and how it gets used in practice.  Using the #2 as the depth for the calling class is not really a "sure bet" as far as I am aware
        // generate a folder name based on the calling class (the Test class name) - fully qualified name is used
        String folderName =  ReflectionUtils.getCallingClass(2).getName()

        //Add Template Files
        // @TODO refactor
        URL bpmnjsResource =  getClass().getResource(getLocalBpmnViewerPath())
        Path bpmnjsFilePath = Paths.get(bpmnjsResource.getPath())
        Path fontAwesome = getLocalFontAwesomePath()

        CssGeneration cssGeneration = new CssGeneration()
        JsGeneration jsGeneration = new JsGeneration()
        Path cssFile = cssGeneration.getCssFilePath()
        Path jsFile = jsGeneration.getJsFilePath()
        treeBuilder {
            "${buildDir}" {
                "bpmn-coverage" {
                    "bpmnjs" {
                        file(jsFile.getFileName().toString(), jsFile.newInputStream().getText('UTF-8'))
                        file(cssFile.getFileName().toString(), cssFile.newInputStream().getText('UTF-8'))
                        file(bpmnjsFilePath.getFileName().toString(), bpmnjsFilePath.newInputStream().getText('UTF-8'))
                    }
                }
            }
        }

        //Copy fonts folder
        File fontsFolderSource = Paths.get(getClass().getResource("/bpmnjs/font-awesome").toURI()).toFile()
        File fontsFolderDestination = Paths.get("${buildDir}/bpmn-coverage/bpmnjs/font-awesome").toFile()
        FileUtils.copyDirectory(fontsFolderSource, fontsFolderDestination, false)


        // Generate coverageData
        data.eachWithIndex { key, value, index ->
            // Generate the compiled template using the CoverageData
            String output = compileTemplate(value)
            // Determine if the coverage data name is a UUID of a custom name
            Closure<Boolean> isUUID = {
                try {
                    UUID.fromString(value.name)
                    return true
                } catch(all) {
                    return false
                }
            }
            // Setup the file name based on whether the coverage data name is a UUID
            Closure<String> fileName = {
                if (isUUID()) {
                    return "${index}.html"
                } else {
                    return "${index}_${key}.html"
                }
            }
            // Generate the file output for the coverage
            treeBuilder {
                "${buildDir}" {
                    "bpmn-coverage" {
                        "${folderName}" {
                            file(fileName(), output)
                        }
                    }
                }
            }
        }
    }

    Map<String, Integer> getActivityEvents(ProcessInstance processInstance){
        String processInstanceId = processInstance.getProcessInstanceId()

        def events = historyService()
                .createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .orderPartiallyByOccurrence()
                .asc()
                .list()
        def activityEvents = events.findAll {it.activityType != 'sequenceFlow' && it.activityType != 'multiInstanceBody'}
                .collect {it.activityId}
                .countBy {it}
        return activityEvents
    }

    List<String> getUnfinishedActivityEvents(ProcessInstance processInstance){
        String processInstanceId = processInstance.getProcessInstanceId()

        def eventsStillActive = historyService()
                .createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .unfinished()
                .orderPartiallyByOccurrence()
                .asc()
                .list()
        def activityEventsStillActive = eventsStillActive.findAll {it.activityType != 'sequenceFlow'}
                .collect {it.activityId}

        return activityEventsStillActive
    }

    List<String> getFinishedSequenceFlows(ProcessInstance processInstance){
        String processInstanceId = processInstance.getProcessInstanceId()

        def events = historyService()
                .createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .activityType('sequenceFlow')
                .orderPartiallyByOccurrence()
                .asc()
                .list()

        List<String> sequenceFlows = events.collect {it.activityId}

        return sequenceFlows
    }

    BpmnModelInstance getBpmnModelInstanceFromDb(ProcessInstance processInstance){
        String processInstanceId = processInstance.getProcessInstanceId()

        String processDefinitionId = historyService()
                .createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult()
                .getProcessDefinitionId()

        BpmnModelInstance model = repositoryService().getBpmnModelInstance(processDefinitionId)
        return model
    }

    List<Map<String,Object>> getAsyncConfigsFromModel(BpmnModelInstance bpmnModelInstance){
        def asyncData = bpmnModelInstance.getModelElementsByType(FlowNode.class).collect {[
                'id': it.getId(),
                'asyncBefore': it.isCamundaAsyncBefore().toBoolean(),
                'asyncAfter': it.isCamundaAsyncAfter().toBoolean(),
                'exclusive': it.isCamundaExclusive().toBoolean()
        ]}
        return asyncData
    }

    List<String> getUserTasksFromModel(BpmnModelInstance bpmnModelInstance){
        bpmnModelInstance.getModelElementsByType(UserTask.class).collect {it.getId()}
    }
    List<String> getReceiveTasksFromModel(BpmnModelInstance bpmnModelInstance){
        bpmnModelInstance.getModelElementsByType(ReceiveTask.class).collect {it.getId()}
    }
    List<String> getExternalTasksFromModel(BpmnModelInstance bpmnModelInstance){
        bpmnModelInstance.getModelElementsByType(ServiceTask.class).findAll {it.getCamundaType() == 'external'}.collect {it.getId()}
    }
    List<String> getIntermediateCatchEventsFromModel(BpmnModelInstance bpmnModelInstance){
        bpmnModelInstance.getModelElementsByType(IntermediateCatchEvent.class).collect {it.getId()}
    }

    List<Map<String, Object>> getVariableActivityFromDb(ProcessInstance processInstance){
        String processInstanceId = processInstance.getProcessInstanceId()

        Collection<HistoricDetail> variableHistory = historyService().createHistoricDetailQuery()
                .processInstanceId(processInstanceId)
                .disableBinaryFetching()
                .variableUpdates()
                .list()

        ArrayList<Map<String, Object>> activityVariableMappings = variableHistory.collect { historyItem ->
            historyItem = (HistoricDetailVariableInstanceUpdateEntity)historyItem
            [('activityId'): historyService().createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .activityInstanceId(historyItem.getActivityInstanceId())
                    .singleResult()
                    .getActivityId(),
             ('variableInstance') : [
                     'variableValue':historyItem.getValue(),
                     'variableName' : historyItem.getVariableName(),
                     'variableType':historyItem.getVariableTypeName() ]
            ]
        }
        return activityVariableMappings
    }

    CoverageData generateCoverageData(ProcessInstance processInstance, String coverageDataName = null, Integer coverageDataWeight = 0){
        CoverageData coverageData = new CoverageData()
        coverageData.name = coverageDataName
        coverageData.weight = coverageDataWeight
        coverageData.activityInstancesFinished = getActivityEvents(processInstance)
        coverageData.activityInstancesUnfinished = getUnfinishedActivityEvents(processInstance)
        coverageData.sequenceFlowsFinished = getFinishedSequenceFlows(processInstance)

        BpmnModelInstance model = getBpmnModelInstanceFromDb(processInstance)
        coverageData.modelAsyncData = getAsyncConfigsFromModel(model)
        coverageData.modelUserTasks = getUserTasksFromModel(model)
        coverageData.modelReceiveTasks = getReceiveTasksFromModel(model)
        coverageData.modelExternalTasks = getExternalTasksFromModel(model)
        coverageData.modelIntermediateCatchEvents = getIntermediateCatchEventsFromModel(model)
        coverageData.bpmnModel = Bpmn.convertToString(model)
        coverageData.activityInstanceVariableMapping = getVariableActivityFromDb(processInstance)

        return coverageData
    }

    String compileTemplate(CoverageData coverageData){
//        def head = generateTemplateHead()
        def body = generateTemplateBody(coverageData)
//        def footer = generateTemplateFooter()
//      @TODO Update Template Generation code to be a cleaner usage of scripting

        return """
        ${body}
        """
    }


}