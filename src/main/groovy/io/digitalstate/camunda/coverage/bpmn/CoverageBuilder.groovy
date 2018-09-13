package io.digitalstate.camunda.coverage.bpmn

import groovy.json.StringEscapeUtils
import io.digitalstate.camunda.coverage.bpmn.bpmnjs.TemplateGeneration
import org.camunda.bpm.engine.history.HistoricDetail
import org.camunda.bpm.engine.runtime.ProcessInstance
import org.camunda.bpm.model.bpmn.Bpmn
import org.camunda.bpm.model.bpmn.BpmnModelInstance
import org.camunda.bpm.model.bpmn.instance.FlowNode
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent
import org.camunda.bpm.model.bpmn.instance.ReceiveTask
import org.camunda.bpm.model.bpmn.instance.ServiceTask
import org.camunda.bpm.model.bpmn.instance.UserTask
import org.codehaus.groovy.reflection.ReflectionUtils

import java.nio.file.Path
import java.nio.file.Paths

import static groovy.json.JsonOutput.toJson
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.historyService
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.repositoryService

trait CoverageBuilder implements TemplateGeneration{

    static HashMap<String, CoverageData> coverageSnapshots = [:]

    void coverageSnapshot(ProcessInstance processInstance, String coverageDataName = UUID.randomUUID(), Integer coverageDataWeight = 0){
        CoverageData coverageData = generateCoverageData(processInstance, coverageDataName, coverageDataWeight)
        coverageSnapshots.put(coverageDataName, coverageData)
    }

    void saveCoverageSnapshots(Boolean useCdn = getBpmnViewerCdnUseState(), HashMap<String, CoverageData> data = coverageSnapshots, String buildDir = 'target') {
        FileTreeBuilder treeBuilder = new FileTreeBuilder()

        // @TODO review use cases of reflection and how it gets used in practice.  Using the #2 as the depth for the calling class is not really a "sure bet"
        // generate a folder name based on the calling class (the Test class name) - fully qualified
        String folderName =  ReflectionUtils.getCallingClass(2).getName()

         // @TODO implement lots of cleanup around how CDN vs local file generation is implemented in this method and the coveragebulder/templateGeneration traits
        if (!useCdn) {
            setUseBpmnViewerCdn(false)
            URL bpmnjsResource =  getClass().getResource(getLocalBpmnViewerPath())
            InputStream bpmnJsInputStream = bpmnjsResource.newInputStream()
            Path bpmnjsFilePath = Paths.get(bpmnjsResource.getPath())
            String bpmnJsFileName = bpmnjsFilePath.getFileName().toString()

            treeBuilder {
                "${buildDir}" {
                        "bpmn-coverage" {
                            "${folderName}" {
                                "bpmnjs" {
                                    file(bpmnJsFileName, bpmnJsInputStream.getText('UTF-8'))
                                }
                            }
                        }
                    }
                }
            }

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

    String convertBpmnModelInstanceToJsReadyString(BpmnModelInstance bpmnModelInstance){
        StringEscapeUtils.escapeJavaScript(Bpmn.convertToString(bpmnModelInstance))
    }

    List<Map<String, Object>> getVariableActivityFromDb(ProcessInstance processInstance){
        String processInstanceId = processInstance.getProcessInstanceId()

        Collection<HistoricDetail> variableHistory = historyService().createHistoricDetailQuery()
                                                                    .processInstanceId(processInstanceId)
                                                                    .disableBinaryFetching()
                                                                    .variableUpdates()
                                                                    .list()

        ArrayList<Map<String, Object>> activityVariableMappings = variableHistory.collect { historyItem ->
            [('activityId'): historyService().createHistoricActivityInstanceQuery()
                                            .processInstanceId(processInstanceId)
                                            .activityInstanceId(historyItem.getActivityInstanceId())
                                            .singleResult()
                                            .getActivityId(),
             ('variableInstance') : historyItem.toString()
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
        coverageData.bpmnModel = convertBpmnModelInstanceToJsReadyString(model)
        coverageData.activityInstanceVariableMapping = getVariableActivityFromDb(processInstance)

        return coverageData
    }

   String compileTemplate(CoverageData coverageData){
       def head = generateTemplateHead()
       def body = generateTemplateBody(
               "${UUID.randomUUID().toString().replaceAll("\\W", "")}", // Creates a UUID for the coverage name for uniqueness and removes all hyphens
               coverageData.bpmnModel,
               toJson(coverageData.modelUserTasks),
               toJson(coverageData.activityInstancesFinished),
               toJson(coverageData.sequenceFlowsFinished),
               toJson(coverageData.modelAsyncData),
               toJson(coverageData.modelReceiveTasks),
               toJson(coverageData.modelExternalTasks),
               toJson(coverageData.modelIntermediateCatchEvents),
               toJson(coverageData.activityInstancesUnfinished),
               toJson(coverageData.activityInstanceVariableMapping)
       )
       def footer = generateTemplateFooter()
//      @TODO Update Template Generation code to be a cleaner usage of scripting

       return """
        ${head}
        ${body}
        ${footer}
        """
   }


}