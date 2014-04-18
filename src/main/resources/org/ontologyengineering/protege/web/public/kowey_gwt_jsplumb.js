function gwtjsplumbinit() {
    jsPlumb.Defaults.Connector = [ "Bezier", { curviness:25 } ];
    jsPlumb.Defaults.DragOptions = { cursor: "pointer", zIndex:2000 };
    jsPlumb.Defaults.PaintStyle = { strokeStyle:"gray", lineWidth:2 };
    jsPlumb.Defaults.EndpointStyle = { radius:3, fillStyle:"gray" };
    jsPlumb.Defaults.Anchors =  [ "AutoDefault", "AutoDefault" ];
    jsPlumb.Defaults.Container = $("body");
}

// if labelId or labelText are null we do not provide a label
function connect_pair(source, target, labelId, labelText) {
    gwtjsplumbinit();
    var fillColor = "gray";
    var arrowCommon = { foldback:0.3, fillStyle:fillColor, width:8 };
    // use three-arg spec to create two different arrows with the common values:
    var overlays = [[ "Arrow", { location:1 }, arrowCommon ]];
    if (labelId != null && labelText != null) {
        overlays.push( ["Custom", { create:function(component) {
                                        return $("<input type='text' class='gwt-TextBox' id='" +
                                                  labelId + "' style='width: 6em;' value='" +
                                                  labelText + "' placeholder='PROPERTY'></input>"); },
                                    location:0.7,
                                    id:"customOverlay" }] );
    }

    return jsPlumb.connect({source:source, target:target, overlays:overlays});
}

function disconnect(conn) {
    jsPlumb.detach(conn); // this seems to raise an exception
}

function repaint_everything() {
    jsPlumb.recalculateOffsets($("body"));
    jsPlumb.repaintEverything();
}


function make_draggable(draggableId) {
    jsPlumb.draggable($(draggableId));
}

function make_undraggable(draggableId) {
    jsPlumb.setDraggable($(draggableId), false);
}
