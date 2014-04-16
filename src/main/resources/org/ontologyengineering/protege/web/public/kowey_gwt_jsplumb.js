function gwtjsplumbinit() {
         jsPlumb.Defaults.Connector = [ "Bezier", { curviness:25 } ];
         jsPlumb.Defaults.DragOptions = { cursor: "pointer", zIndex:2000 };
         jsPlumb.Defaults.PaintStyle = { strokeStyle:"gray", lineWidth:2 };
         jsPlumb.Defaults.EndpointStyle = { radius:3, fillStyle:"gray" };
         jsPlumb.Defaults.Anchors =  [ "AutoDefault", "AutoDefault" ];
 }

 function gwtjsconnect(pairs) {
     gwtjsplumbinit();
     var fillColor = "gray";
     var arrowCommon = { foldback:0.3, fillStyle:fillColor, width:8 };
     // use three-arg spec to create two different arrows with the common values:
     var overlays = [
             [ "Arrow", { location:1 }, arrowCommon ]
     ];
     $.each(pairs, function(index, value) {
        //alert("gwtjsconnect called! v2 [2014-01-22 11:30]" + value.source + " -> " + value.target);
        jsPlumb.connect({source:value.source, target:value.target, overlays:overlays});
     });

 }

 function connect_pair(source, target) {
      gwtjsconnect([{source: source, target: target}]);
 }

 function make_draggable(draggableId) {
     jsPlumb.draggable($(draggableId));
 }

 function make_undraggable(draggableId) {
     jsPlumb.setDraggable($(draggableId), false);
 }
