package io.digitalstate.camunda.coverage.bpmn.bpmnjs

import groovy.json.JsonOutput
import groovy.text.SimpleTemplateEngine
import io.digitalstate.camunda.coverage.bpmn.CoverageData

import java.nio.file.Path
import java.nio.file.Paths

class TemplateGeneration {

    Path bpmnJsViewer = Paths.get(getClass().getResource('/bpmnjs/bpmn-navigated-viewer.development-2.1.0.js').toURI())
    Path fontAwesomeCss = Paths.get(getClass().getResource('/bpmnjs/font-awesome/css/font-awesome.min.css').toURI())
    JsGeneration jsGeneration
    CssGeneration cssGeneration

    TemplateGeneration(JsGeneration jsGeneration, CssGeneration cssGeneration){
        setJsGeneration(jsGeneration)
        setCssGeneration(cssGeneration)
    }

    def generateTemplateBody(CoverageData coverageData){
        // Feature Name
        String uuid = UUID.randomUUID().toString().replaceAll("\\W", "")

        def coverageDataPrep = [
                'featureName' : uuid,
                'xml' : coverageData.bpmnModel,
                'userTasks' : coverageData.modelUserTasks,
                'activityInstances' : coverageData.activityInstancesFinished,
                'executedSequenceFlows' : coverageData.sequenceFlowsFinished,
                'asyncData' : coverageData.modelAsyncData,
                'receiveTasks' : coverageData.modelReceiveTasks,
                'externalTasks' : coverageData.modelExternalTasks,
                'intermediateCatchEvents' : coverageData.modelIntermediateCatchEvents,
                'activityInstancesStillActive' : coverageData.activityInstancesUnfinished,
                'activityInstanceVariableMapping' : coverageData.activityInstanceVariableMapping,
                'template': '/templates/template.html',
                'bpmnViewer' : "../bpmnjs/${getBpmnJsViewer().getFileName().toString()}",
                'bpmnShowDiagramJs' : "../bpmnjs/${getJsGeneration().getJsFilePath().getFileName().toString()}",
                "bpmnCSS" : "../bpmnjs/${getCssGeneration().getCssFilePath().getFileName().toString()}",
                'bpmnFontAwesome': "../bpmnjs/${getFontAwesomeCss().getFileName().toString()}"
        ]

        def binding = [
                'data' : coverageDataPrep,
                'jsonData': JsonOutput.toJson(coverageDataPrep)
        ]

        String template = getClass().getResource(coverageDataPrep.template).getText()
        def engine = new SimpleTemplateEngine()
        String rendered = engine.createTemplate(template).make(binding)

        return rendered
    }


    //
    // SETTERS AND GETTERS
    //
    void setFontAwesomeCss(String path){
        this.fontAwesomeCss = Paths.get(getClass().getResource(path).toURI())
    }

    Path getFontAwesomeCss(){
        return this.fontAwesomeCss
    }

    void setBpmnJsViewer(String path){
        this.bpmnJsViewer = Paths.get(getClass().getResource(path).toURI())
    }

    Path getBpmnJsViewer(){
        return this.bpmnJsViewer
    }

    void setJsGeneration(JsGeneration jsGeneration){
        this.jsGeneration = jsGeneration
    }

    JsGeneration getJsGeneration(){
        return this.jsGeneration
    }

    void setCssGeneration(CssGeneration cssGeneration){
        this.cssGeneration = cssGeneration
    }
}