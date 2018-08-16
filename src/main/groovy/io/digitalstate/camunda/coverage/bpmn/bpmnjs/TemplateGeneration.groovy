package io.digitalstate.camunda.coverage.bpmn.bpmnjs

import groovy.text.SimpleTemplateEngine
import io.digitalstate.camunda.coverage.bpmn.Helpers

trait TemplateGeneration implements Helpers{

    String templateDir = 'templates'

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

    def generateTemplateHead(String file = 'head.html'){
        return resourceStream("${getTemplateDir()}/${file}").getText()
    }

    def generateTemplateFooter(String file = 'footer.html'){
        return resourceStream("${getTemplateDir()}/${file}").getText()
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
               'activityInstanceVariableMapping':activityInstanceVariableMapping
        ]

        String template = resourceStream("${getTemplateDir()}/${file}").getText()
        def engine = new SimpleTemplateEngine()

        String rendered = engine.createTemplate(template).make(binding)

        return rendered

    }

}