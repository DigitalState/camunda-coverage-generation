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

    // @TODO Rebuild Trait as cleaner implementation.  Too much code is in here

trait CoverageBuilder{

    static HashMap<String, CoverageData> coverageSnapshots = [:]
    static CssGeneration cssGeneration = new CssGeneration()
    static JsGeneration jsGeneration = new JsGeneration()

    void coverageSnapshot(ProcessInstance processInstance,
                          String coverageDataName = UUID.randomUUID(),
                          Integer coverageDataWeight = 0){
        CoverageData coverageData = generateCoverageData(processInstance, coverageDataName, coverageDataWeight)
        coverageSnapshots.put(coverageDataName, coverageData)
    }

    void saveCoverageSnapshots(HashMap<String, CoverageData> data = coverageSnapshots,
                               String buildDir = 'target',
                               TemplateGeneration templateGeneration = new TemplateGeneration(jsGeneration, cssGeneration)) {

        FileTreeBuilder treeBuilder = new FileTreeBuilder()

        // @TODO review use cases of reflection and how it gets used in practice.  Using the #2 as the depth for the calling class is not really a "sure bet" as far as I am aware
        // generate a folder name based on the calling class (the Test class name) - fully qualified name is used
        String folderName =  ReflectionUtils.getCallingClass(2).getName()

        //Add Template Files
        String jsFileName = getJsGeneration().getJsFileName()
        String JsFile = getJsGeneration().generateJs()

        String cssFileName = getCssGeneration().getCssFileName()
        String cssFile = getCssGeneration().generateCss()

        String bpmnJsViewerFileName = templateGeneration.getBpmnJsViewerFileName()
        String bpmnJsViewerFile = templateGeneration.getBpmnJsViewerFile()

        treeBuilder {
            "${buildDir}" {
                "bpmn-coverage" {
                    "bpmnjs" {
                        file(jsFileName, JsFile)
                        file(cssFileName, cssFile)
                        file(bpmnJsViewerFileName, bpmnJsViewerFile)
                    }
                }
            }
        }

        //Copy fonts folder
        String fontsFolderPath = '/bpmnjs/font-awesome/'
//        URL codeurl = getClass().getProtectionDomain().getCodeSource().getLocation()
//        File fontsFolderSource = new File("jar:${getClass().getProtectionDomain().getCodeSource().getLocation()}!${fontsFolderPath}");
        File fontsFolderSource = new File(getClass().getResource(fontsFolderPath).toURI())
        File fontsFolderDestination = Paths.get("${buildDir}/bpmn-coverage/bpmnjs/font-awesome").toFile()
        FileUtils.copyDirectory(fontsFolderSource, fontsFolderDestination, false)

        // Generate coverageData
        data.eachWithIndex { key, value, index ->
            // Generate the compiled template using the CoverageData
            String output = templateGeneration.generateTemplateBody(value)
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

    CoverageData generateCoverageData(ProcessInstance processInstance,
                                      String coverageDataName = null,
                                      Integer coverageDataWeight = 0){

        BpmnModelInstance model = getBpmnModelInstanceFromDb(processInstance)

        CoverageData coverageData = new CoverageData().with { cd ->
            cd.name = coverageDataName
            cd.weight = coverageDataWeight
            cd.activityInstancesFinished = getActivityEvents(processInstance)
            cd.activityInstancesUnfinished = getUnfinishedActivityEvents(processInstance)
            cd.sequenceFlowsFinished = getFinishedSequenceFlows(processInstance)
            cd.modelAsyncData = getAsyncConfigsFromModel(model)
            cd.modelUserTasks = getUserTasksFromModel(model)
            cd.modelReceiveTasks = getReceiveTasksFromModel(model)
            cd.modelExternalTasks = getExternalTasksFromModel(model)
            cd.modelIntermediateCatchEvents = getIntermediateCatchEventsFromModel(model)
            cd.bpmnModel = Bpmn.convertToString(model)
            cd.activityInstanceVariableMapping = getVariableActivityFromDb(processInstance)
            return cd
        }
        return coverageData
    }

//    String compileTemplate(CoverageData coverageData){
//        def body = getTemplateGeneration().generateTemplateBody(coverageData)
//        return body
//    }



    // @TODO Move processors into their own class/or Trait.
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

    //
    // SETTERS AND GETTERS
    //
    void setCssGeneration(CssGeneration cssGeneration){
        this.cssGeneration = cssGeneration
    }

    CssGeneration getCssGeneration(){
        return cssGeneration
    }

    void setJsGeneration(JsGeneration jsGeneration){
        this.jsGeneration = jsGeneration
    }

    JsGeneration getJsGeneration(){
        return jsGeneration
    }
    }