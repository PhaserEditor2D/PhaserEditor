function main() {
    new PhaserEditor2D.Editor();
}
window.addEventListener("keydown", function (e) {
    PhaserEditor2D.Editor.getInstance().sendKeyDown(e);
    e.preventDefault();
    e.stopImmediatePropagation();
});
window.addEventListener("load", main);
