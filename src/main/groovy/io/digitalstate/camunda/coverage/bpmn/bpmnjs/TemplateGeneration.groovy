package io.digitalstate.camunda.coverage.bpmn.bpmnjs

import groovy.json.JsonOutput
import groovy.text.SimpleTemplateEngine
import io.digitalstate.camunda.coverage.bpmn.CoverageData

import java.nio.file.Path
import java.nio.file.Paths

trait TemplateGeneration{

    // @TODO convert to class

    String localBpmnViewerPath = '/bpmnjs/bpmn-navigated-viewer.development-2.1.0.js'
    String defaultFontAwesome = '/bpmnjs/font-awesome/css/font-awesome.min.css'
    Path localFontAwesomePath = Paths.get(getClass().getResource(this.defaultFontAwesome).toURI())

    void setLocalFontAwesomePath(String customPath){
        this.localFontAwesomePath = Paths.get(customPath)
    }

    Path getLocalFontAwesomePath(){
        return this.localFontAwesomePath
    }

    void setLocalBpmnViewerPath(String path){
        this.localBpmnViewerPath = path
    }

    String getLocalBpmnViewerPath(){
        return this.localBpmnViewerPath
    }

    def generateTemplateBody(CoverageData coverageData){
        // Feature Name
        String uuid = UUID.randomUUID().toString().replaceAll("\\W", "")

        def coverageDataPrep = [
                'featureName' : uuid ,
                'xml' : coverageData.bpmnModel ,
                'userTasks' : coverageData.modelUserTasks ,
                'activityInstances' : coverageData.activityInstancesFinished ,
                'executedSequenceFlows' : coverageData.sequenceFlowsFinished ,
                'asyncData' : coverageData.modelAsyncData ,
                'receiveTasks' : coverageData.modelReceiveTasks ,
                'externalTasks' : coverageData.modelExternalTasks ,
                'intermediateCatchEvents' : coverageData.modelIntermediateCatchEvents ,
                'activityInstancesStillActive' : coverageData.activityInstancesUnfinished ,
                'activityInstanceVariableMapping' : coverageData.activityInstanceVariableMapping ,
                'template': '/templates/template.html',
                'bpmnViewer' : "..${this.localBpmnViewerPath}" ,
                'bpmnShowDiagramJs' : "../bpmnjs/${new JsGeneration().getJsFilePath().getFileName().toString()}" ,
                "bpmnCSS" : "../bpmnjs/${new CssGeneration().getCssFilePath().getFileName().toString()}",
                'bpmnFontAwesome': "..${this.defaultFontAwesome}"
        ]

        def binding = [
                'data' : coverageDataPrep,
                'jsonData': JsonOutput.toJson(coverageDataPrep)
        ]

        // @TODO Cleanup to make better code.  This is too messy atm.
        String template = getClass().getResource(coverageDataPrep.template).getText()
        def engine = new SimpleTemplateEngine()
        String rendered = engine.createTemplate(template).make(binding)

        return rendered
    }
}