package io.digitalstate.camunda.coverage.bpmn.bpmnjs

import groovy.text.SimpleTemplateEngine
import org.apache.commons.io.FilenameUtils

class CssGeneration {

    private final String defaultCssFile = '/templates/bpmn.css'
    String cssFileName = FilenameUtils.getName(this.defaultCssFile)
    String cssFile = getClass().getResourceAsStream(this.defaultCssFile).getText('UTF-8')

    void setCssFile(InputStream inputStream){
        this.cssFile = inputStream.getText('UTF-8')
    }

    String getCssFileName(){
        return this.cssFileName
    }
    void setCssFileName(String fileName){
        this.cssFileName = fileName
    }

    String generateCss(){
        String template = this.cssFile
        def engine = new SimpleTemplateEngine()
        String rendered = engine.createTemplate(template).make()
        return rendered
    }
}
