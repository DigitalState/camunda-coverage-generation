package io.digitalstate.camunda.coverage.bpmn.bpmnjs

import groovy.json.JsonOutput
import groovy.text.SimpleTemplateEngine
import io.digitalstate.camunda.coverage.bpmn.CoverageData
import org.apache.commons.io.FilenameUtils

class TemplateGeneration {

    String defaultBpmnJsViewer = "/bpmnjs/bpmn-navigated-viewer.development-2.1.0.js"
    String bpmnJsViewerFileName = FilenameUtils.getName(this.defaultBpmnJsViewer)
    String bpmnJsViewerFile = getClass().getResourceAsStream(this.defaultBpmnJsViewer).getText('UTF-8')

    String defaultFontAwesomeCss = "/bpmnjs/font-awesome/css/font-awesome.min.css"
    String fontAwesomeCssFileName = FilenameUtils.getName(this.defaultFontAwesomeCss)
    String fontAwesomeCssFile = getClass().getResourceAsStream(this.defaultFontAwesomeCss).getText('UTF-8')

    String defaultTemplate = '/templates/template.html'
    String templateFileName = FilenameUtils.getName(this.defaultTemplate)
    String templateFile = getClass().getResourceAsStream(this.defaultTemplate).getText('UTF-8')

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
                'bpmnViewer' : "../bpmnjs/${getBpmnJsViewerFileName()}",
                'bpmnShowDiagramJs' : "../bpmnjs/${getJsGeneration().getJsFileName()}",
                "bpmnCSS" : "../bpmnjs/${getCssGeneration().getCssFileName()}",
                'bpmnFontAwesome': "../bpmnjs/${getFontAwesomeCssFileName()}"
        ]

        def binding = [
                'data' : coverageDataPrep,
                'jsonData': JsonOutput.toJson(coverageDataPrep)
        ]

        String template = getTemplateFile()
        def engine = new SimpleTemplateEngine()
        String rendered = engine.createTemplate(template).make(binding)

        return rendered
    }


    //
    // SETTERS AND GETTERS
    //
    void setJsGeneration(JsGeneration jsGeneration){
        this.jsGeneration = jsGeneration
    }

    JsGeneration getJsGeneration(){
        return this.jsGeneration
    }

    CssGeneration getCssGeneration(){
        return this.cssGeneration
    }

    void setCssGeneration(CssGeneration cssGeneration){
        this.cssGeneration = cssGeneration
    }

    String getFontAwesomeCssFile(){
        return this.fontAwesomeCssFile
    }
    void setFontAwesomeCssFile(InputStream inputStream){
        this.fontAwesomeCssFile = inputStream
    }

    String getFontAwesomeCssFileName(){
        return this.fontAwesomeCssFileName
    }
    void setFontAwesomeCssFileName(String filename){
        this.fontAwesomeCssFileName = filename
    }

    String getBpmnJsViewerFile(){
        return this.bpmnJsViewerFile
    }

    void setBpmnJsViewerFile(InputStream inputStream){
        this.bpmnJsViewerFile = inputStream
    }

    String getBpmnJsViewerFileName(){
        return this.bpmnJsViewerFileName
    }
    void setBpmnJsViewerFileName(String filename){
        this.bpmnJsViewerFileName = filename
    }

    void setTemplateFile(InputStream inputStream){
        this.templateFile = inputStream.getText('UTF-8')
    }
    String getTemplateFile(){
        return this.templateFile
    }



}