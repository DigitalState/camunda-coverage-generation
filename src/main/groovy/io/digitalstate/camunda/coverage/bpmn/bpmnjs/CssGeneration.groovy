package io.digitalstate.camunda.coverage.bpmn.bpmnjs

import groovy.text.SimpleTemplateEngine

import java.nio.file.Path
import java.nio.file.Paths

class CssGeneration {

    String defaultCssFile = '/templates/bpmn.css'
    Path cssFilePath = Paths.get(getClass().getResource(this.defaultCssFile).toURI())

    void setCssFile(String customPath){
        this.cssFilePath = Paths.get(customPath)
    }

    Path getCssFilePath(){
        return this.cssFilePath
    }

    String getCssFileName(){
        return this.cssFilePath.getFileName().toString()
    }

    String generateCss(){
        String template = this.cssFilePath.newInputStream().getText('UTF-8')
        def engine = new SimpleTemplateEngine()
        String rendered = engine.createTemplate(template).make()
        return rendered
    }
}
