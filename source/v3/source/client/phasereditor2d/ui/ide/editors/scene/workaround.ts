window.addEventListener("onerror", function (e) {
    alert("WebView ERROR: " + e);
});

// needed to fix errors in MacOS SWT Browser.
(<any>window).AudioContext = function () {};


// disable log on production
var CONSOLE_LOG = false;
function consoleLog(msg: any) {
    if (CONSOLE_LOG) {
        console.log(msg);
    }
}


namespace phasereditor2d.ui.ide.editors.scene {
    export function isLeftButton(e : MouseEvent) {
        if (e.buttons === undefined) {
            // macos swt browser
            return e.button === 0;
        }

        return e.buttons === 1;
    }

    export function isMiddleButton(e : MouseEvent) {
        if (e.buttons === undefined) {
            // macos swt browser
            return e.button === 1;
        }

        return e.buttons === 4;
    }
}

// missing types in Phaser definitions

class ActiveXObject {

}

namespace Phaser.Types.Tweens {
    export class StaggerBuilderConfig {
        
    }
}