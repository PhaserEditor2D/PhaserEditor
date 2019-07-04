window.addEventListener("onerror", function (e) {
    alert("WebView ERROR: " + e);
});
window.AudioContext = function () { };
var CONSOLE_LOG = false;
function consoleLog(msg) {
    if (CONSOLE_LOG) {
        console.log(msg);
    }
}
var PhaserEditor2D;
(function (PhaserEditor2D) {
    function isLeftButton(e) {
        if (e.buttons === undefined) {
            return e.button === 0;
        }
        return e.buttons === 1;
    }
    PhaserEditor2D.isLeftButton = isLeftButton;
    function isMiddleButton(e) {
        if (e.buttons === undefined) {
            return e.button === 1;
        }
        return e.buttons === 4;
    }
    PhaserEditor2D.isMiddleButton = isMiddleButton;
})(PhaserEditor2D || (PhaserEditor2D = {}));
