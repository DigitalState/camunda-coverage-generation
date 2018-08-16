package io.digitalstate.camunda.coverage.bpmn

trait Helpers{

    // helper method to shorten the .addInputStream params in createDeployment()
    static InputStream resourceStream(String path){
        return this.getClassLoader().getResource(path.toString()).newInputStream()
    }
}