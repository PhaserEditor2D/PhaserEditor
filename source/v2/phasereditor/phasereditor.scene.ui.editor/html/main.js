function main() {
    channel = getChannelId();

    console.log("Starting renderer [channel=" + channel + "]");

    console.log("Opening socket " + getWebSocketUrl());

    var websocket = new WebSocket(getWebSocketUrl());  
      
    var editor = new Editor(websocket);
}

function getChannelId() {
    var s = document.location.search;
    var i = s.indexOf("=");
    var c = s.substring(i + 1);
    return c;
}

function getWebSocketUrl() {
    var loc = document.location;
    return "ws://" + loc.host + "/ws/api?channel=" + channel;
}

window.addEventListener("load", main);