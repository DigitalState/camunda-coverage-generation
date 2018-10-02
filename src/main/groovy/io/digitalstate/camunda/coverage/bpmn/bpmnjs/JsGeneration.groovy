package io.digitalstate.camunda.coverage.bpmn.bpmnjs

import groovy.text.SimpleTemplateEngine

import java.nio.file.Path
import java.nio.file.Paths

class JsGeneration {

    String defaultJsFile = '/templates/showDiagram.js'
    Path jsFilePath = Paths.get(getClass().getResource(this.defaultJsFile).toURI())

    void setJsFile(String customPath){
        this.cssFilePath = Paths.get(customPath)
    }

    Path getJsFilePath(){
        return this.jsFilePath
    }

    String getJsFileName(){
        return this.jsFilePath.getFileName().toString()
    }

    String generateJs(){
        String template = this.jsFilePath.newInputStream().getText('UTF-8')
        def engine = new SimpleTemplateEngine()
        String rendered = engine.createTemplate(template).make()
        return rendered
    }
}
