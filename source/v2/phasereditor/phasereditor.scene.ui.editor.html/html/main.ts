function main() {
    new PhaserEditor2D.Editor();
}

window.addEventListener("keydown", function (e) {
    e.preventDefault();
    e.stopImmediatePropagation();
});


window.addEventListener("load", main);


var CONSOLE_LOG = false;
function consoleLog(msg: any) {
    if (CONSOLE_LOG) {
        console.log(msg);
    }
}