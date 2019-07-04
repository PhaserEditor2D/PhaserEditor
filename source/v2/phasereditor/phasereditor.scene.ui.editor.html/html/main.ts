function main() {
    new PhaserEditor2D.Editor();
}

window.addEventListener("keydown", function (e : KeyboardEvent) {
    PhaserEditor2D.Editor.getInstance().sendKeyDown(e);
    e.preventDefault();
    e.stopImmediatePropagation();
});

window.addEventListener("keyup", function (e : KeyboardEvent) {
    // I don't know why the ESC key is not captured in the keydown.
    if (e.keyCode === 27) {
        PhaserEditor2D.Editor.getInstance().sendKeyDown(e);
    }    
    e.preventDefault();
    e.stopImmediatePropagation();
});

window.addEventListener("load", main);