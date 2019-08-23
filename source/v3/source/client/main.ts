namespace phasereditor2d.demo {

    export function main() {
        console.log("Booting!!!");
        ui.Workbench.getWorkbench();
    }
}

window.addEventListener("load", () => {
    phasereditor2d.demo.main();
});