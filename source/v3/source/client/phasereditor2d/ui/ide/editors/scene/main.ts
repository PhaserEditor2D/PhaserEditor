namespace phasereditor2d.ui.ide.editors.scene {

    function main() {
        new Editor();
    }
    
    window.addEventListener("keydown", function (e : KeyboardEvent) {
        Editor.getInstance().sendKeyDown(e);
        e.preventDefault();
        e.stopImmediatePropagation();
    });
    
    window.addEventListener("keyup", function (e : KeyboardEvent) {
        // I don't know why the ESC key is not captured in the keydown.
        if (e.keyCode === 27) {
            Editor.getInstance().sendKeyDown(e);
        }    
        e.preventDefault();
        e.stopImmediatePropagation();
    });
    
    window.addEventListener("load", main);

}
