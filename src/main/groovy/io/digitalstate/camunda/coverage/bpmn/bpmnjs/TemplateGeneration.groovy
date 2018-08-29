package io.digitalstate.camunda.coverage.bpmn.bpmnjs

import groovy.text.SimpleTemplateEngine
import io.digitalstate.camunda.coverage.bpmn.Helpers

trait TemplateGeneration implements Helpers{

    String templateDir = 'templates'
    String bpmnViewerUrl = 'https://unpkg.com/bpmn-js@2.1.0/dist/bpmn-navigated-viewer.development.js'
    Boolean useBpmnViewerCdn = true
    String localBpmnViewerPath = './bpmnjs/bpmn-navigated-viewer.development-2.1.0.js'

    /**
     *
     * @param templateDir relative to resources folder.  No leading slash
     */
    void setTemplateDir(String templateDir){
        this.templateDir = templateDir
    }

    String getTemplateDir(){
        return this.templateDir
    }

    void setBpmnViewerUrl(String url){
        this.bpmnViewerUrl = url
    }

    String getBpmnViewerUrl(){
        return this.bpmnViewerUrl
    }

    void setLocalBpmnViewerPath(String path){
        this.localBpmnViewerPath = path
    }

    String getLocalBpmnViewerPath(){
        return this.localBpmnViewerPath
    }

    void useBpmnViewerCdn(Boolean useCdn){
        this.useBpmnViewerCdn = useCdn
    }

    Boolean getBpmnViewerCdnUseState(){
        return this.useBpmnViewerCdn
    }


    def generateTemplateHead(String file = 'head.html'){
        return resourceStream("${getTemplateDir()}/${file}").getText()
    }

    def generateTemplateFooter(String file = 'footer.html'){
        return resourceStream("${getTemplateDir()}/${file}").getText()
    }

    String getBpmnViewer(){
        if (this.useBpmnViewerCdn){
            return this.bpmnViewerUrl
        } else {
            return this.localBpmnViewerPath
        }
    }

    def generateTemplateBody( String featureName = '',
                              String xml = '',
                              userTasks = [],
                              activityInstances =[],
                              executedSequenceFlows =[],
                              asyncData =[],
                              receiveTasks =[],
                              externalTasks =[],
                              intermediateCatchEvents =[],
                              activityInstancesStillActive =[],
                              activityInstanceVariableMapping =[],
                              String file = 'body.html'){

        def binding = [
               'featureName': featureName,
               'xml': xml,
               'userTasks': userTasks,
               'activityInstances': activityInstances,
               'executedSequenceFlows': executedSequenceFlows,
               'asyncData': asyncData,
               'receiveTasks': receiveTasks,
               'externalTasks': externalTasks,
               'intermediateCatchEvents': intermediateCatchEvents,
               'activityInstancesStillActive': activityInstancesStillActive,
               'activityInstanceVariableMapping':activityInstanceVariableMapping,
               'bpmnViewer': getBpmnViewer()
        ]

        String template = resourceStream("${getTemplateDir()}/${file}").getText()
        def engine = new SimpleTemplateEngine()

        String rendered = engine.createTemplate(template).make(binding)

        return rendered

    }

}