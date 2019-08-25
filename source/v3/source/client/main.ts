namespace phasereditor2d.demo {

    export function main() {
        console.log("Starting workbench.");
        const workbench = ui.Workbench.getWorkbench();
        workbench.start();
    }
}

window.addEventListener("load", () => {
    phasereditor2d.ui.controls.Controls.preload(

        () => { 
           phasereditor2d.demo.main(); 
        }

    );
});