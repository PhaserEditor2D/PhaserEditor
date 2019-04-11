function main() {
    new PhaserEditor2D.Editor();
}
window.addEventListener("keydown", function (e) {
    e.preventDefault();
    e.stopImmediatePropagation();
});
window.addEventListener("load", main);
