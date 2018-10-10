package io.digitalstate.camunda.coverage.bpmn.bpmnjs

import groovy.text.SimpleTemplateEngine
import org.apache.commons.io.FilenameUtils

class JsGeneration {

    private String defaultJsFile = '/templates/showDiagram.js'
    String jsFileName = FilenameUtils.getName(this.defaultJsFile)
    String jsFile = getClass().getResourceAsStream(this.defaultJsFile).getText("UTF-8")

    void setJsFile(InputStream inputStream){
        this.jsFile = inputStream.getText('UTF-8')
    }

    String getJsFileName(){
        return this.jsFileName
    }

    void setJsFileName(String fileName){
        this.jsFileName = fileName
    }

    String generateJs(){
        String template = this.jsFile
        def engine = new SimpleTemplateEngine()
        String rendered = engine.createTemplate(template).make()
        return rendered
    }

}
