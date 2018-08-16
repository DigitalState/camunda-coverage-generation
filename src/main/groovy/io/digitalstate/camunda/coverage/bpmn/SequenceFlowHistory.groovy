package io.digitalstate.camunda.coverage.bpmn

import org.camunda.bpm.model.bpmn.Bpmn
import org.camunda.bpm.model.bpmn.BpmnModelInstance
import org.camunda.bpm.model.bpmn.instance.SequenceFlow
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaExecutionListener
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaScript

trait SequenceFlowHistory implements Helpers{

    public String sequenceFlowListenerScriptPath = 'sequenceflowhistory/sequenceFlowHistoryEventGenerator.js'

    InputStream getSequenceFlowListenerScript(){
        return resourceStream(this.sequenceFlowListenerScriptPath)
    }

    public String getSequenceFlowFileName(){
        return sequenceFlowListenerScriptPath.substring(sequenceFlowListenerScriptPath.lastIndexOf('/') + 1)
    }


    private static BpmnModelInstance setupSequenceFlowListeners(BpmnModelInstance model, String scriptResource, String scriptFormat){
        def sequenceFlows = model.getModelElementsByType(SequenceFlow.class).collect {it.getId()}
        BpmnModelInstance newModel = model
        sequenceFlows.each {
            newModel = addExecutionListener(newModel, it, scriptResource, scriptFormat)
        }
        return newModel
    }

    private static BpmnModelInstance addExecutionListener(BpmnModelInstance model, String elementId, String scriptResource, String scriptFormat){
        CamundaExecutionListener extLis = model.newInstance(CamundaExecutionListener.class)
        CamundaScript camScript = model.newInstance(CamundaScript.class)
        camScript.setCamundaResource(scriptResource)
        camScript.setCamundaScriptFormat(scriptFormat)
        extLis.setCamundaEvent('take')
        extLis.setCamundaScript(camScript)

        BpmnModelInstance newModel = model.getModelElementById(elementId).builder().addExtensionElement(extLis).done()
        return newModel
    }

    static BpmnModelInstance addSequenceFlowListeners( String modelPath,
                                                   String scriptResource = 'deployment://sequenceFlowHistoryEventGenerator.js',
                                                   String scriptFormat = 'javascript') {

        InputStream resource = resourceStream(modelPath)
        BpmnModelInstance model = Bpmn.readModelFromStream(resource)
        BpmnModelInstance preppedModel = setupSequenceFlowListeners(model, scriptResource, scriptFormat)
        return preppedModel
    }
}