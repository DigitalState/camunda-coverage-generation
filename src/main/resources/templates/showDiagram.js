function showDiagram(data) {
    var viewer = new BpmnJS({
        container: '#diagram' + data.featureName
        });

    var diagramXML = data.xml // Wrapped in quotes to ensure XML is a string
    var userTasks = data.userTasks
    var activityInstances = data.activityInstances
    var executedSequenceFlows = data.executedSequenceFlows
    var asyncData = data.asyncData
    var receiveTasks = data.receiveTasks
    var externalTasks = data.externalTasks
    var intermediateCatchEvents = data.intermediateCatchEvents
    var activityInstancesStillActive = data.activityInstancesStillActive
    var activityInstanceVariableMapping = data.activityInstanceVariableMapping


    viewer.importXML(diagramXML, function() {
    var overlays = viewer.get('overlays'),
        canvas = viewer.get('canvas'),
        elementRegistry = viewer.get('elementRegistry');

         canvas.zoom('fit-viewport');

    // Count of Activity Instance activation
    for (var key in activityInstances) {
        var activityInstance = key;
        canvas.addMarker(activityInstance, 'highlight');
        overlays.add(key, {
                    position: {
                        bottom: 0,
                        right: 0
                    },
                    html: '<div class="activity-instance-count">' + activityInstances[key] + '</div>'
                    });
    }

    // Variable Execution Instances
    for (var i = 0; i < activityInstanceVariableMapping.length; i++) {
        var varMapping = activityInstanceVariableMapping[i]['activityId'];
        overlays.add(varMapping, {
                    position: {
                        top: -15,
                        right: 0
                    },
                    html: '<div class="activity-instance-variable-execution">' + '<i class="fa fa-pencil-square-o"></i>' +'</div>'
                    });
    }

    // Activity Instances: unFinished (Still Running)
    for (var i = 0; i < activityInstancesStillActive.length; i++) {
        var activityInstanceStillActive = activityInstancesStillActive[i];
        canvas.addMarker(activityInstanceStillActive, 'highlight-running');
    }

    // Executed Sequence Flows
    for (var i = 0; i < executedSequenceFlows.length; i++) {
        var sequenceFlow = executedSequenceFlows[i];
        canvas.addMarker(sequenceFlow, 'highlight-connection');
    }

    // UserTasks Boundaries
    for (var i = 0; i < userTasks.length; i++) {
        var task = userTasks[i];
        overlays.add(task, 'note', {
        position: {
            bottom: 40,
            left: -4
        },
        html: '<div class="user-transaction-boundary"></div>'
        });
    }

    // Receive Tasks Boundaries
    for (var i = 0; i < receiveTasks.length; i++) {
        var task = receiveTasks[i];
        overlays.add(task, 'note', {
        position: {
            bottom: 40,
            left: -4
        },
        html: '<div class="receivetask-transaction-boundary"></div>'
        });
    }

    // ExternalTasks Tasks Boundaries
    for (var i = 0; i < externalTasks.length; i++) {
        var task = externalTasks[i];
        overlays.add(task, 'note', {
        position: {
            bottom: 40,
            left: -4
        },
        html: '<div class="externaltask-transaction-boundary"></div>'
        });
    }

    // Intermediate Catch Events Boundaries
    for (var i = 0; i < intermediateCatchEvents.length; i++) {
        var event = intermediateCatchEvents[i];
        overlays.add(event, 'note', {
        position: {
            bottom: 18,
            left: -4
        },
        html: '<div class="intermediatecatchevent-transaction-boundary"></div>'
        });
    }

    // Async Boundaries
    for (var i = 0; i < asyncData.length; i++) {
        var item = asyncData[i];
        // asyncBefore
        if (item.asyncBefore === true){
            overlays.add(item['id'], 'note', {
            position: {
                bottom: 40,
                left: -4
            },
            html: '<div class="transaction-boundary"></div>'
            });
        }
        // AsyncAfter
        if (item.asyncAfter === true){
            overlays.add(item['id'], 'note', {
            position: {
                bottom: 40,
                right: -1
            },
            html: '<div class="transaction-boundary"></div>'
            });
        }

        // TODO add Exclusive Marker. Data is already in object `task.exclusvie == true/false`
    }


    }); // end of ImportXML
}