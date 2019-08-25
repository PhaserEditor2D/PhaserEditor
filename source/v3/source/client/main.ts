namespace phasereditor2d.demo {

    export function main() {
        console.log("Booting!!!");
        ui.Workbench.getWorkbench();
    }
}

window.addEventListener("load", () => {
    phasereditor2d.ui.controls.Controls.preload(

        () => phasereditor2d.demo.main()

    );
});